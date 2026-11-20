package org.vros.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.application.service.OptimizationService;
import org.vros.contracts.optimization.OptimizationResultEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationResultListener {

    private final OptimizationService optimizationService;

    @KafkaListener(
            topics = "${vros.kafka.topics.results}",
            groupId = "${vros.kafka.groups.api}",
            containerFactory = "optimizationListenerContainerFactory")
    public void onResult(ConsumerRecord<String, OptimizationResultEvent> record) {
        if (record.value() == null) {
            log.warn("Skipping unreadable result event key={} partition={} offset={}",
                    record.key(), record.partition(), record.offset());
            return;
        }
        log.info("Received result for job {} ({} routes, feasible={})",
                record.value().jobId(), record.value().routes() == null ? 0 : record.value().routes().size(),
                record.value().feasible());
        optimizationService.handleResult(record.value());
    }
}
