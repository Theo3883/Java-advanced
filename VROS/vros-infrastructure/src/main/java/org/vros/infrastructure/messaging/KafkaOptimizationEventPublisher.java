package org.vros.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.vros.application.port.OptimizationEventPublisher;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.domain.exception.OptimizationSubmissionException;
import org.vros.infrastructure.config.OptimizationKafkaProperties;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class KafkaOptimizationEventPublisher implements OptimizationEventPublisher {

    private final KafkaTemplate<String, Object> optimizationKafkaTemplate;
    private final OptimizationKafkaProperties properties;

    @Override
    public void publishRequest(OptimizationRequestEvent event) {
        try {
            optimizationKafkaTemplate
                    .send(properties.getTopics().getRequests(), event.jobId().toString(), event)
                    .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OptimizationSubmissionException("Interrupted while publishing optimization request", ex);
        } catch (Exception ex) {
            throw new OptimizationSubmissionException("Could not publish optimization request", ex);
        }
    }
}
