package org.vros.application.port;

import org.vros.contracts.optimization.OptimizationRequestEvent;

public interface OptimizationEventPublisher {

    void publishRequest(OptimizationRequestEvent event);
}
