package org.vros.worker.solver;

import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;

/**
 * Solves a capacitated VRP snapshot and produces a result event.
 *
 * <p>Implementations must be stateless so a single bean can serve concurrent solver threads.</p>
 */
public interface VrpSolver {

    OptimizationResultEvent solve(OptimizationRequestEvent request, ProgressListener progressListener);

    /**
     * Called by the solver while it assigns deliveries. Implementations may throttle, but must
     * never rely on the listener for correctness.
     */
    @FunctionalInterface
    interface ProgressListener {

        void onProgress(int progress, String phase, String message);

        ProgressListener NOOP = (progress, phase, message) -> {
        };
    }
}
