package org.vros.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.application.service.OptimizationService;
import org.vros.contracts.optimization.OptimizationProgressEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationProgressListener {

    private final OptimizationService optimizationService;
    private final OptimizationMetrics metrics;

    @KafkaListener(
            topics = "${vros.kafka.topics.progress}",
            groupId = "${vros.kafka.groups.api}",
            containerFactory = "optimizationListenerContainerFactory")
    public void onProgress(ConsumerRecord<String, OptimizationProgressEvent> record) {
        if (record.value() == null) {
            log.warn("Skipping unreadable progress event key={} partition={} offset={}",
                    record.key(), record.partition(), record.offset());
            return;
        }
        metrics.progressEvent();
        optimizationService.handleProgress(record.value());
    }
}
