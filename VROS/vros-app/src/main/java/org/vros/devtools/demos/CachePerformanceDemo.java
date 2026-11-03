package org.vros.devtools.demos;

import lombok.extern.slf4j.Slf4j;
import org.vros.domain.model.Delivery;
import org.vros.application.service.DeliveryService;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;

import java.util.List;

@Slf4j
public final class CachePerformanceDemo {

    private CachePerformanceDemo() {}

    public static void run(DeliveryService deliveryService, ApplicationContext context) {
        log.info("\n------------------------------------------------------------------");
        log.info(" [DEMO 3] Application-Level Caching (Cold vs Warm Read)");
        log.info("------------------------------------------------------------------");

        // Evict cache to guarantee a fresh Cold Read
        CacheManager cacheManager = context.getBean(CacheManager.class);
        var cache = cacheManager.getCache("deliveries");
        if (cache != null) {
            cache.clear();
        }

        // 1. Cold Read (Hits Database)
        long startCold = System.nanoTime();
        List<Delivery> coldList = deliveryService.findAll();
        long elapsedColdNs = System.nanoTime() - startCold;
        double coldMs = elapsedColdNs / 1_000_000.0;

        // 2. Warm Read (Hits Caffeine in-memory cache)
        long startWarm = System.nanoTime();
        List<Delivery> warmList = deliveryService.findAll();
        long elapsedWarmNs = System.nanoTime() - startWarm;
        double warmMs = elapsedWarmNs / 1_000_000.0;

        double improvementPercent = coldMs > 0 ? ((coldMs - warmMs) / coldMs) * 100.0 : 0.0;

        log.info("Read Performance Measurement ({} items):", coldList.size());
        log.info("  1. Cold Read (Database Roundtrip) : {} ms", String.format("%.3f", coldMs));
        log.info("  2. Warm Read (Caffeine Cache Hit) : {} ms", String.format("%.3f", warmMs));
        log.info("  -> Speedup Improvement             : {}% faster with Caffeine cache!", String.format("%.1f", improvementPercent));
    }
}
