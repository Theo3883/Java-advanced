package org.vros.worker.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.worker.service.OptimizationOrchestrator;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationRequestListener {

    private final OptimizationOrchestrator orchestrator;

    @KafkaListener(
            topics = "${vros.kafka.topics.requests}",
            groupId = "${vros.kafka.groups.worker}",
            concurrency = "${vros.worker.listener-concurrency:3}")
    public void onRequest(ConsumerRecord<String, OptimizationRequestEvent> record) {
        log.debug("Received optimization request topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        orchestrator.handle(record.value());
    }
}
