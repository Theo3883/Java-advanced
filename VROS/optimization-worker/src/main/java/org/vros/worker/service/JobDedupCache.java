package org.vros.worker.service;

import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.OptimizationResultEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded in-memory dedup cache for Kafka redeliveries.
 *
 * <p>The worker stays stateless in the database sense: the cache only short-circuits duplicate
 * deliveries of the same job within one process. Entries expire after one hour and the cache is
 * capped so a long-running worker cannot grow without bounds.</p>
 */
@Component
public class JobDedupCache {

    private static final int MAX_ENTRIES = 10_000;
    private static final Duration TTL = Duration.ofHours(1);

    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public Optional<OptimizationResultEvent> completedResult(UUID jobId) {
        Entry entry = entries.get(jobId);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            entries.remove(jobId, entry);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.result());
    }

    public boolean isInProgress(UUID jobId) {
        Entry entry = entries.get(jobId);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            entries.remove(jobId, entry);
            return false;
        }
        return entry.result() == null;
    }

    public void markInProgress(UUID jobId) {
        evictIfNeeded();
        entries.put(jobId, new Entry(Instant.now(), null));
    }

    public void markCompleted(OptimizationResultEvent result) {
        evictIfNeeded();
        entries.put(result.jobId(), new Entry(Instant.now(), result));
    }

    public void remove(UUID jobId) {
        entries.remove(jobId);
    }

    int size() {
        return entries.size();
    }

    private void evictIfNeeded() {
        if (entries.size() < MAX_ENTRIES) {
            return;
        }
        entries.entrySet().removeIf(entry -> entry.getValue().isExpired());
        if (entries.size() >= MAX_ENTRIES) {
            entries.keySet().stream().limit(MAX_ENTRIES / 10L).toList().forEach(entries::remove);
        }
    }

    private record Entry(Instant updatedAt, OptimizationResultEvent result) {

        boolean isExpired() {
            return updatedAt.plus(TTL).isBefore(Instant.now());
        }
    }
}
