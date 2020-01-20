package com.redhat.summit.ukipam.fuse;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class CamelRouter extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // @formatter:off
        from("direct:checkTransactions")
            .log("todo - read from kafka")
            .to("direct:checkRules")
            .to("direct:riskRating");

        from("direct:checkRules")
            .log("Sending block alert for '${body}'...")
            .marshal().json(JsonLibrary.Jackson)
            .log("QUESTIONS; whats the Decision Service API? Do we have a Swagger spec?")
            .to("direct:todo");

        from("direct:riskRating")
            .choice()
                .when().simple("${body.isHigh()}")
                .to("direct:sendBlockAlert")
            .otherwise()
                .to("direct:createCase")
            .end();

        from("direct:sendBlockAlert")
            .log("Sending block alert for '${body}'...")
            .marshal().json(JsonLibrary.Jackson)
            .to("kafka:block_account");

        from("direct:createCase")
            .log("Calling case management for '${body}'...")
            .marshal().json(JsonLibrary.Jackson)
            .log("QUESTIONS; whats the Case Management API? Do we have a Swagger spec?")
            .to("direct:todo");
        // @formatter:on
    }
}