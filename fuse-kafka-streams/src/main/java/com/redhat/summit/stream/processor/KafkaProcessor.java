package com.redhat.summit.stream.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("${kafka.enabled:true}")
public class KafkaProcessor implements ProcessorSupplier {

    @Autowired
    ObjectMapper mapper;

    @Override
    public Processor get() {
        Processor processor = new Processor<String, String>() {

            @Override
            public void init(ProcessorContext processorContext) {
                log.info("Kafka processor started");
            }

            @Override
            public void process(String key, String payload) {
                try {
                    log.info("STREAM :: Processing Message :: {}", payload);

                    if (StringUtils.isNotBlank(payload)) {
                        final ObjectNode node = mapper.readValue(payload, ObjectNode.class);

                        if (node.has("amount")) {
                            long amount = node.get("amount").asLong();

                            if (amount > 1) {
                                log.info("STREAM :: Fraud Event Detected :: {}", payload);
                                log.info("POSSIBLE FRAUD EVENT :: Transaction amount exceeds defaults with {}", amount);

                                // TODO: Add Drools / DMN Invocation Logic
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            @Override
            public void close() {
                log.info("Kafka processor closed");
            }
        };
        return processor;
    }
}
