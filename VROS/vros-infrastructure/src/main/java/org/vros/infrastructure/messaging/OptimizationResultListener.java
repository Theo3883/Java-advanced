package org.vros.infrastructure.messaging;

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
import org.vros.application.service.OptimizationService;
import org.vros.contracts.optimization.OptimizationResultEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationResultListener {

    private final OptimizationService optimizationService;
    private final OptimizationMetrics metrics;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(
                    delayString = "${vros.kafka.result-retry-backoff-ms:1000}",
                    multiplierString = "2.0",
                    maxDelayString = "${vros.kafka.result-retry-max-backoff-ms:10000}"),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            exclude = DeserializationException.class)
    @KafkaListener(
            topics = "${vros.kafka.topics.results}",
            groupId = "${vros.kafka.groups.api}",
            containerFactory = "optimizationListenerContainerFactory")
    public void onResult(ConsumerRecord<String, OptimizationResultEvent> record) {
        if (record.value() == null) {
            throw new DeserializationException(
                    "Unreadable optimization result payload", null, false,
                    new IllegalArgumentException("Missing OptimizationResultEvent value"));
        }
        log.info("Received result for job {} ({} routes, feasible={})",
                record.value().jobId(), record.value().routes() == null ? 0 : record.value().routes().size(),
                record.value().feasible());
        metrics.resultCompleted();
        optimizationService.handleResult(record.value());
    }
}
