package org.vros.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.application.service.OptimizationService;
import org.vros.contracts.optimization.OptimizationAckEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationAckListener {

    private final OptimizationService optimizationService;
    private final OptimizationMetrics metrics;

    @KafkaListener(
            topics = "${vros.kafka.topics.acks}",
            groupId = "${vros.kafka.groups.api}",
            containerFactory = "optimizationListenerContainerFactory")
    public void onAck(ConsumerRecord<String, OptimizationAckEvent> record) {
        if (record.value() == null) {
            log.warn("Skipping unreadable ACK event key={} partition={} offset={}",
                    record.key(), record.partition(), record.offset());
            return;
        }
        log.debug("Received ACK for job {}: {}", record.value().jobId(), record.value().status());
        metrics.ackReceived(record.value().status());
        optimizationService.handleAck(record.value());
    }
}
