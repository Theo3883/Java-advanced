package org.vros.worker.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.worker.exception.UnsupportedSchemaVersionException;
import org.vros.worker.service.OptimizationOrchestrator;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationRequestListener {

    private final OptimizationOrchestrator orchestrator;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(
                    delayString = "${vros.worker.listener-retry-backoff-ms:1000}",
                    multiplierString = "2.0",
                    maxDelayString = "${vros.worker.listener-retry-max-backoff-ms:10000}"),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true",
            autoStartDltHandler = "false",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            exclude = {DeserializationException.class, UnsupportedSchemaVersionException.class})
    @KafkaListener(
            topics = "${vros.kafka.topics.requests}",
            groupId = "${vros.kafka.groups.worker}",
            concurrency = "${vros.worker.listener-concurrency:3}")
    public void onRequest(ConsumerRecord<String, OptimizationRequestEvent> record) {
        log.debug("Received optimization request topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        if (record.value() == null) {
            // The ErrorHandlingDeserializer stored the failure in the headers and delivered a
            // null value. Re-throw a DeserializationException so the excluded path sends it
            // straight to the DLT instead of retrying an unreadable payload.
            throw new DeserializationException(
                    "Unreadable optimization request payload", null, false,
                    new IllegalArgumentException("Missing OptimizationRequestEvent value"));
        }
        orchestrator.handle(record.value());
    }
}
