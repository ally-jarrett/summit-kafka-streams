package com.redhat.summit.test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.TransactionEventConfiguration;
import com.redhat.summit.processor.TransactionEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@DirtiesContext
public class KafkaTransactionProducerTest {

    private static String SENDER_TOPIC = "test-topic-in";

    @Autowired
    private CamelContext camelContext;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private TransactionEventProcessor eventProcessor;

    @Produce(uri = "direct:start")
    protected ProducerTemplate testProducer;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private KafkaMessageListenerContainer<String, String> container;
    private BlockingQueue<ConsumerRecord<String, String>> records;
    Object groupId;
    Object bootstrapServers;
    protected int count = 1;

    @ClassRule
    public static EmbeddedKafkaRule embeddedKafka = new
            EmbeddedKafkaRule(1, true, SENDER_TOPIC).kafkaPorts(9092);

    @Before
    public void setUp() throws Exception {
        // set up the Kafka consumer properties
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("hello-world-consumer", "false",
                embeddedKafka.getEmbeddedKafka());
        for (Entry<String, Object> entry : consumerProperties.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            if (entry.getKey().equals("group.id")) {
                groupId = entry.getValue();
            } else if (entry.getKey().equals("bootstrap.servers")) {
                bootstrapServers = entry.getValue();
            }
        }

        // create a Kafka consumer factory
        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<String, String>(consumerProperties,
                        new StringDeserializer(), new StringDeserializer());

        // set the topic that needs to be consumed
        ContainerProperties containerProperties = new ContainerProperties(SENDER_TOPIC);

        // create a Kafka MessageListenerContainer
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        // create a thread safe queue to store the received message
        records = new LinkedBlockingQueue<>();

        // setup a Kafka message listener
        container.setupMessageListener(new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                log.info("Message Number '{}' Received", count);
                log.debug("test-listener received message='{}'", record.toString());
                records.add(record);
                count++;
            }
        });

        // start the container and underlying message listener
        container.start();

        // wait until the container has the required number of assigned partitions
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getEmbeddedKafka().getPartitionsPerTopic());

        String kafkaServer = "kafka:localhost:9092";
        String zooKeeperHost = "zookeeperHost=localhost&zookeeperPort=2181";
        String serializerClass = "serializerClass=kafka.serializer.StringEncoder";
        String embedded = new StringBuilder()
                .append(bootstrapServers).append("?")
                .append(SENDER_TOPIC).append("&")
                .append(zooKeeperHost).append("&")
                .append(serializerClass).toString();

        // Add mock route for testing
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").process(eventProcessor).to("direct:kafkaStart");
            }
        });
        camelContext.start();

    }

    @After
    public void tearDown() {
        container.stop();
    }


    @Test
    public void testCamelKafkaProducer() throws Exception {
        TransactionEventConfiguration eventConfiguration = new TransactionEventConfiguration();
        eventConfiguration.setCardHolderName("Ally");
        eventConfiguration.setCardNumber("1234-5678-9101");
        eventConfiguration.setTotalAmount(1000l);
        eventConfiguration.setTotalTransactionCount(100);
        testProducer.sendBody(eventConfiguration);

        // Wait for the listener to gather all messages
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    log.info("Record Count {}", records.size());
                    return records.size() == eventConfiguration.getTotalTransactionCount();
                });

        assertNotNull(records);
        assertEquals(eventConfiguration.getTotalTransactionCount(), records.size());

        long avgTxAmount = eventConfiguration.getTotalAmount() / eventConfiguration.getTotalTransactionCount();
        ConsumerRecord<String, String> received;
        while (!records.isEmpty()) {
            received = records.take();
            assertNotNull(received);

            CreditCardTransaction tx = mapper.readValue(received.value(), CreditCardTransaction.class);
            assertNotNull(received);
            assertNotNull(tx.getTransactionReference());
            assertEquals(eventConfiguration.getCardHolderName(), tx.getCardHolderName());
            assertEquals(eventConfiguration.getCardNumber(), tx.getCardNumber());
            assertEquals(avgTxAmount, tx.getAmount());
        }
    }
}