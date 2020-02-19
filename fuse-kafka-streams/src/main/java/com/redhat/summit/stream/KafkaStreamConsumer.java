//package com.redhat.summit.stream;
//
//import com.redhat.summit.stream.processor.KafkaProcessor;
//import com.redhat.summit.util.KafkaProperties;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.streams.KafkaStreams;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.kstream.KStream;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import java.util.Properties;
//
//@Service
//@Slf4j
//@ConditionalOnExpression("${kafka.enabled:true}")
//public class KafkaStreamConsumer {
//
//    @Autowired
//    private KafkaProperties kafkaProperties;
//
//    @Autowired
//    private KafkaProcessor processor;
//
//    @PostConstruct
//    public void processKafkaConsumer() {
//        Properties properties = kafkaProperties.getConsumerProperties();
//        KafkaStreams kafkaStreams = null;
//        try {
//
//            // Configure/Build Stream
//            StreamsBuilder builder = new StreamsBuilder();
//            KStream<String, String> kStream = builder.stream(kafkaProperties.getKafkaProducerTestTopic());
//            kStream.process(processor, new String[0]);
//            kStream.to(kafkaProperties.getKafkaConsumerTestTopic());
//
//            // Start Stream
//            kafkaStreams = new KafkaStreams(builder.build(), properties);
//            kafkaStreams.start();
//            log.info("op={}, status=OK, desc={}", "KafkaConsumer", "kafka consumer stream  started successfully");
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            log.error("op={}, status=KO, desc={} and exception={}",
//                    new Object[]{"KafkaConsumer", "exception while starting kafka consumer stream", ex.getMessage()});
//            if (kafkaStreams != null) {
//                kafkaStreams.close();
//            }
//        }
//    }
//}
