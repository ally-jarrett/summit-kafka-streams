//package com.redhat.summit.stream;
//
////import com.redhat.summit.stream.processor.KafkaProcessor;
//
//import com.redhat.summit.stream.processor.KafkaProcessor;
//import com.redhat.summit.util.KafkaProperties;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.math.NumberUtils;
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.streams.KafkaStreams;
//import org.apache.kafka.streams.KeyValue;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.StreamsConfig;
//import org.apache.kafka.streams.kstream.KStream;
//import org.apache.kafka.streams.kstream.TimeWindows;
//import org.apache.kafka.streams.kstream.Windowed;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
//import org.springframework.context.annotation.Bean;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.annotation.EnableKafkaStreams;
//import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import java.util.AbstractMap;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//

//@Slf4j
//@Service
//@ConditionalOnExpression("${kafka.enabled:true}")
//public class KafkaCCMaxValueEventStream {
//
//    @Autowired
//    private KafkaProperties kafkaProperties;
//
//    @Autowired
//    private KafkaProcessor processor;
//
//    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
//    public StreamsConfig kStreamsConfigs(KafkaProperties kafkaProperties) {
//        return new StreamsConfig(kafkaProperties.getConsumerProperties());
//    }
//
//    @Bean
//    public KStream<String, String> kStream(StreamsBuilder builder) {
//
//        //EventProcessor rulesApplier = new EventProcessor();
//        //final KStream<String, String> inputTopic = builder.stream(kafkaProperties.getKafkaProducerTestTopic());
//
//        // Configure/Build Stream
//        KStream<String, String> kStream = builder.stream(kafkaProperties.getKafkaProducerTestTopic());
//        kStream.process(processor, new String[0]);
//        kStream.to(kafkaProperties.getKafkaConsumerTestTopic());
//        log.info("op={}, status=OK, desc={}", "KafkaConsumer", "kafka consumer stream  started successfully");
//
////        KStream<Windowed<String>, Long>[] sream = inputTopic.map((x, y) -> new KeyValue<>(x,rulesApplier.processEvent(x,y)))
////                .filter((k,v) -> null != v)
////                .groupByKey()
////                .windowedBy(TimeWindows.of(60000L))
////                .count()
////                .filter((k,v) -> v > 2)
////                .toStream()
////                .branch((k,v) -> null != v);
////        sream[0].foreach((k,v) -> System.out.println(k.key()));
////        KStream<String, String> stream = sream[0]
////                .map((k,v)-> new KeyValue<>(k.key(),String.valueOf(v)));
//
////        stream.to(kafkaProperties.getKafkaConsumerTestTopic());
//
//        return kStream;
//    }
//}
