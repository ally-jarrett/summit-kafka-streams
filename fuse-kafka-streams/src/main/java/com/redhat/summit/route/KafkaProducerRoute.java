package com.redhat.summit.route;

import com.redhat.summit.util.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@ConditionalOnExpression("${kafka.enabled:true}")
public class KafkaProducerRoute extends RouteBuilder {

    @Autowired
    private KafkaProperties kafkaProperties;

    String testKafkaMessage = "Test Message from  MessagePublisherClient :: ";

    @Override
    public void configure() throws Exception {

        // @formatter:off

        log.info("Bootstrapping Kafka Producer Routes, Autostart producer is set to: {} ",
                kafkaProperties.isAutoStartProducer());

        // Produce message
        from("direct:kafkaStart").routeId("Camel::Kafka::Producer")
                .setHeader(KafkaConstants.PARTITION_KEY, simple("0"))
                .setHeader(KafkaConstants.KEY, simple("1"))
                .log(LoggingLevel.INFO, "PRODUCER  :: " + kafkaProperties.getKafkaProducerTestTopic()
                        + " :: CC TX Count - ${body.size()} :: Delay Timer per Message - ${header.delayTimer} ms")
                .log(LoggingLevel.DEBUG, "Sending Kafka Message Headers: ${headers}")
                .split(body()) // Split Array
                    .delay(header("delayTimer")) // Add delay per message
                    .marshal().json(JsonLibrary.Jackson) // Marshall to JSON
                    .to(kafkaProperties.kafkaTestProducerUri()); // to Kafka!

        // @formatter:on
    }

    private String getMessageBody() {
        return this.testKafkaMessage + UUID.randomUUID().toString();
    }
}
