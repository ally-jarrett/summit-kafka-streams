package com.redhat.summit.route;

import com.redhat.summit.util.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnExpression("${kafka.enabled:true}")
public class KafkaConsumerRoute extends RouteBuilder {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Override
    public void configure() throws Exception {

        // @formatter:off
        log.info("Bootstrapping Kafka Consumer Routes");

        from(kafkaProperties.kafkaTestConsumerUri())
                .routeId("Camel::Kafka::Consumer")
                .log(LoggingLevel.INFO, "CONSUMER  :: " + kafkaProperties.getKafkaConsumerTestTopic() + " :: ${body}")
                .to("mock:consumer-endpoint");

        // @formatter:on
    }
}
