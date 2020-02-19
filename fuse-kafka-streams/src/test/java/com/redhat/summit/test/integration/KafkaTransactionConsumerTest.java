package com.redhat.summit.test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.TransactionEventConfiguration;
import com.redhat.summit.processor.TransactionEventProcessor;
import com.redhat.summit.util.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@DirtiesContext
public class KafkaTransactionConsumerTest {

    private static String RECEIVER_TOPIC = "test-topic-out";

    @EndpointInject(uri = "mock:consumer")
    protected MockEndpoint out;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    KafkaProperties kafkaProperties;

    @Autowired
    private TransactionEventProcessor eventProcessor;

    @Produce(uri = "direct:start")
    protected ProducerTemplate testProducer;


    @ClassRule
    public static EmbeddedKafkaRule embeddedKafka = new
            EmbeddedKafkaRule(1, true, RECEIVER_TOPIC).kafkaPorts(9092);

    @Before
    public void setUp() throws Exception {

        // Add mock route for testing
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").process(eventProcessor).to("direct:kafkaStart");
            }
        });

        AdviceWithRouteBuilder mockKafkaProducer = new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Override producer endpoint to send directly to consumer
                interceptSendToEndpoint(kafkaProperties.kafkaTestProducerUri())
                        .skipSendToOriginalEndpoint()
                        .to(kafkaProperties.getKafkaConnectionUri(RECEIVER_TOPIC));
            }
        };

        AdviceWithRouteBuilder mockKafkaConsumer = new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                // mock the for testing
                interceptSendToEndpoint("mock:consumer-endpoint")
                        .skipSendToOriginalEndpoint()
                        .to(out);
            }
        };

        camelContext.getRouteDefinition("Camel::Kafka::Producer").adviceWith(camelContext, mockKafkaProducer);
        camelContext.getRouteDefinition("Camel::Kafka::Consumer").adviceWith(camelContext, mockKafkaConsumer);
        camelContext.start();

    }

    @Test
    public void testCamelKafkaConsumer() throws Exception {
        TransactionEventConfiguration eventConfiguration = new TransactionEventConfiguration();
        eventConfiguration.setCardHolderName("Ally");
        eventConfiguration.setCardNumber("1234-5678-9101");
        eventConfiguration.setTotalAmount(1000l);
        eventConfiguration.setTotalTransactionCount(100);
        testProducer.sendBody(eventConfiguration);

        out.expectedMessageCount(100);
        out.assertIsSatisfied();

        long avgTxAmount = eventConfiguration.getTotalAmount() / eventConfiguration.getTotalTransactionCount();
        out.getExchanges().stream().forEach(e -> {
            String v = (String) e.getIn().getBody();
            try {
                CreditCardTransaction tx = mapper.readValue(v, CreditCardTransaction.class);
                assertNotNull(tx.getTransactionReference());
                assertEquals(eventConfiguration.getCardHolderName(), tx.getCardHolderName());
                assertEquals(eventConfiguration.getCardNumber(), tx.getCardNumber());
                assertEquals(avgTxAmount, tx.getAmount());
            } catch (Exception ex) {

            }
        });
    }
}