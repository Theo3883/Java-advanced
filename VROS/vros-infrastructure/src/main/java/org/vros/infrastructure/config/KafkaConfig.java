package org.vros.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.vros.contracts.optimization.OptimizationTopics;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * Jackson 2 mapper used by Spring Kafka's JsonSerializer/JsonDeserializer.
     * Boot 4's default mapper is Jackson 3, which those serializers do not accept.
     */
    @Bean
    public ObjectMapper kafkaObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    public ProducerFactory<String, Object> optimizationProducerFactory(
            KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> config = kafkaProperties.buildProducerProperties();
        config.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        config.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(
                config, new StringSerializer(), new JsonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaTemplate<String, Object> optimizationKafkaTemplate(
            ProducerFactory<String, Object> optimizationProducerFactory) {
        return new KafkaTemplate<>(optimizationProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, Object> optimizationConsumerFactory(
            KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> config = kafkaProperties.buildConsumerProperties();
        config.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(objectMapper);
        jsonDeserializer.addTrustedPackages("org.vros.contracts.optimization");
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> optimizationListenerContainerFactory(
            ConsumerFactory<String, Object> optimizationConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(optimizationConsumerFactory);
        return factory;
    }

    /**
     * Raw-byte consumer for the results DLT: dead-lettered payloads may not be deserializable.
     * The job id is recovered from the record key.
     */
    @Bean
    public ConsumerFactory<String, byte[]> resultsDltConsumerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> config = kafkaProperties.buildConsumerProperties();
        config.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(
                config, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> resultsDltListenerContainerFactory(
            ConsumerFactory<String, byte[]> resultsDltConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(resultsDltConsumerFactory);
        return factory;
    }

    @Bean
    public NewTopic optimizationRequestsTopic() {
        return TopicBuilder.name(OptimizationTopics.REQUESTS).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationAcksTopic() {
        return TopicBuilder.name(OptimizationTopics.ACKS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationResultsTopic() {
        return TopicBuilder.name(OptimizationTopics.RESULTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationProgressTopic() {
        return TopicBuilder.name(OptimizationTopics.PROGRESS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationRequestsDltTopic() {
        return TopicBuilder.name(OptimizationTopics.REQUESTS_DLT).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationResultsDltTopic() {
        return TopicBuilder.name(OptimizationTopics.RESULTS_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationResultsRetry0Topic() {
        return TopicBuilder.name(OptimizationTopics.RESULTS + "-retry-0").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationResultsRetry1Topic() {
        return TopicBuilder.name(OptimizationTopics.RESULTS + "-retry-1").partitions(3).replicas(1).build();
    }
}
