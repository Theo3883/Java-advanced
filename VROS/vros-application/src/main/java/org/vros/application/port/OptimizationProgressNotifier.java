package org.vros.application.port;

import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.domain.model.OptimizationJob;

public interface OptimizationProgressNotifier {

    void notifyProgress(OptimizationProgressEvent event);

    void notifyTerminal(OptimizationJob job);
}
