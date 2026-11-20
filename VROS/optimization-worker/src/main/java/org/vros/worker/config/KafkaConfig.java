package org.vros.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
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
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * Jackson 2 mapper for Spring Kafka serde. Boot 4's default mapper is Jackson 3, which
     * Spring Kafka 4.1's JsonSerializer/JsonDeserializer do not accept.
     */
    @Bean
    public ObjectMapper workerObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    public ProducerFactory<String, Object> optimizationProducerFactory(
            KafkaProperties kafkaProperties, ObjectMapper workerObjectMapper) {
        Map<String, Object> config = kafkaProperties.buildProducerProperties();
        config.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        config.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(
                config, new StringSerializer(), new JsonSerializer<>(workerObjectMapper));
    }

    @Bean
    public KafkaTemplate<String, Object> optimizationKafkaTemplate(
            ProducerFactory<String, Object> optimizationProducerFactory) {
        return new KafkaTemplate<>(optimizationProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, Object> optimizationConsumerFactory(
            KafkaProperties kafkaProperties, ObjectMapper workerObjectMapper) {
        Map<String, Object> config = kafkaProperties.buildConsumerProperties();
        config.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(workerObjectMapper);
        jsonDeserializer.addTrustedPackages("org.vros.contracts.optimization");
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> optimizationConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(optimizationConsumerFactory);
        return factory;
    }

    @Bean
    public NewTopic optimizationRequestsTopic() {
        return TopicBuilder.name("vros.optimization.requests").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationAcksTopic() {
        return TopicBuilder.name("vros.optimization.acks").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationResultsTopic() {
        return TopicBuilder.name("vros.optimization.results").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationProgressTopic() {
        return TopicBuilder.name("vros.optimization.progress").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic optimizationRequestsDltTopic() {
        return TopicBuilder.name("vros.optimization.requests-dlt").partitions(3).replicas(1).build();
    }
}
