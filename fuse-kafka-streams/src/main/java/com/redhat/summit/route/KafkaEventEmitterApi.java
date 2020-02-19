package com.redhat.summit.route;

import com.redhat.summit.model.Ping;
import com.redhat.summit.model.TransactionEventConfiguration;
import com.redhat.summit.processor.TransactionEventProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventEmitterApi extends RouteBuilder {

    @Autowired
    TransactionEventProcessor eventProcessor;

    @Override
    public void configure() throws Exception {

        // @formatter:off

        restConfiguration("servlet")
            .bindingMode(RestBindingMode.json)
            .apiContextPath("/swagger") //swagger endpoint path; Final URL: Camel path + apiContextPath: /api/swagger
            .apiContextRouteId("swagger")
            .contextPath("/api")
            .apiProperty("api.title", "Fraud Detection KafKa REST Event api")
            .apiProperty("api.version", "1.0")
            .apiProperty("host", "");

        rest()
            .get("ping")
                .route().routeId("Camel::REST::GET::Ping")
                .description("Ping Health Endpoint")
                .setBody().body(() -> new Ping())
                .removeHeaders("*")
                .endRest()

            .post("/event").description("Demo POST API.")
                .type(TransactionEventConfiguration.class)
                .route().routeId("Camel::REST::POST::EventEmitter")
                .log(LoggingLevel.INFO, "${body}")
                .process(eventProcessor)
                .to("direct:kafkaStart")
                .endRest();

                // Produce message
//        from("direct:kafkaStart").routeId("Camel::Kafka::TEST")
//                .log(LoggingLevel.INFO, "TRANSACTIONS  :: ${body}");

        // @formatter:on

    }
}