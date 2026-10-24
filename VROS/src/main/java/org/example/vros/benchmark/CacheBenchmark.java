package org.example.vros.benchmark;

import org.example.vros.VrosApplication;
import org.example.vros.service.interfaces.CustomerService;
import org.example.vros.service.interfaces.DeliveryService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark comparing cached vs uncached execution performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 0) // In-process execution so it can be called seamlessly by DemoRunner
public class CacheBenchmark {

    private static final String CONTEXT_KEY = "VROS_APPLICATION_CONTEXT";
    private static ConfigurableApplicationContext sharedContext;

    // Direct interface references
    private CustomerService customerService;
    private DeliveryService deliveryService;
    private CacheManager cacheManager;

    // Resilient reflection handles
    private Object customerServiceTarget;
    private Method customerFindAllMethod;
    private Object deliveryServiceTarget;
    private Method deliveryFindAllMethod;
    private Object cacheManagerTarget;
    private Method getCacheMethod;
    private Method clearCacheMethod;

    public static void setSharedContext(ConfigurableApplicationContext context) {
        sharedContext = context;
        if (context != null) {
            System.getProperties().put(CONTEXT_KEY, context);
        }
    }

    public static ConfigurableApplicationContext getSharedContext() {
        if (sharedContext != null && sharedContext.isActive()) {
            return sharedContext;
        }
        Object obj = System.getProperties().get(CONTEXT_KEY);
        if (obj instanceof ConfigurableApplicationContext ctx && ctx.isActive()) {
            return ctx;
        }
        return null;
    }

    @Setup(Level.Trial)
    public void setUp() {
        ConfigurableApplicationContext ctx = getSharedContext();
        if (ctx == null) {
            ctx = new SpringApplicationBuilder(VrosApplication.class)
                    .web(WebApplicationType.NONE)
                    .properties(
                            "spring.jmx.enabled=false",
                            "spring.application.admin.enabled=false",
                            "spring.sql.init.mode=never",
                            "spring.jpa.hibernate.ddl-auto=validate"
                    )
                    .run();
            setSharedContext(ctx);
        }

        // Try direct type lookup
        try {
            customerService = ctx.getBean(CustomerService.class);
            deliveryService = ctx.getBean(DeliveryService.class);
            cacheManager = ctx.getBean(CacheManager.class);
        } catch (Exception ignored) {
            // Fallback for dual-classloader setups
        }

        // If direct lookup didn't populate, bind via bean names and reflection
        if (customerService == null) {
            bindViaReflection(ctx);
        }
    }

    private void bindViaReflection(ConfigurableApplicationContext ctx) {
        try {
            customerServiceTarget = ctx.getBean("customerServiceImpl");
            customerFindAllMethod = customerServiceTarget.getClass().getMethod("findAll");

            deliveryServiceTarget = ctx.getBean("deliveryServiceImpl");
            deliveryFindAllMethod = deliveryServiceTarget.getClass().getMethod("findAll");

            cacheManagerTarget = ctx.getBean("cacheManager");
            getCacheMethod = cacheManagerTarget.getClass().getMethod("getCache", String.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bind beans in benchmark: " + e.getMessage(), e);
        }
    }

    private void evictCache(String cacheName) {
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.clear();
        } else if (cacheManagerTarget != null && getCacheMethod != null) {
            try {
                Object cache = getCacheMethod.invoke(cacheManagerTarget, cacheName);
                if (cache != null) {
                    if (clearCacheMethod == null) {
                        clearCacheMethod = cache.getClass().getMethod("clear");
                    }
                    clearCacheMethod.invoke(cache);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Cold Read: Cache is evicted before each call, forcing round-trip to database.
     */
    @Benchmark
    public void customerFindAll_CacheCold(Blackhole bh) throws Exception {
        evictCache("customers");
        if (customerService != null) {
            bh.consume(customerService.findAll());
        } else if (customerServiceTarget != null) {
            bh.consume(customerFindAllMethod.invoke(customerServiceTarget));
        }
    }

    /**
     * Warm Read: Result is retrieved from Caffeine in-memory cache.
     */
    @Benchmark
    public void customerFindAll_CacheWarm(Blackhole bh) throws Exception {
        if (customerService != null) {
            bh.consume(customerService.findAll());
        } else if (customerServiceTarget != null) {
            bh.consume(customerFindAllMethod.invoke(customerServiceTarget));
        }
    }

    /**
     * Cold Read for Deliveries: Cache evicted before each call.
     */
    @Benchmark
    public void deliveryFindAll_CacheCold(Blackhole bh) throws Exception {
        evictCache("deliveries");
        if (deliveryService != null) {
            bh.consume(deliveryService.findAll());
        } else if (deliveryServiceTarget != null) {
            bh.consume(deliveryFindAllMethod.invoke(deliveryServiceTarget));
        }
    }

    /**
     * Warm Read for Deliveries: Retrieved from cache.
     */
    @Benchmark
    public void deliveryFindAll_CacheWarm(Blackhole bh) throws Exception {
        if (deliveryService != null) {
            bh.consume(deliveryService.findAll());
        } else if (deliveryServiceTarget != null) {
            bh.consume(deliveryFindAllMethod.invoke(deliveryServiceTarget));
        }
    }

    /**
     * Programmatic trigger for the benchmark
     */
    public static Collection<RunResult> runBenchmark(ConfigurableApplicationContext context) throws RunnerException {
        setSharedContext(context);
        Options opt = new OptionsBuilder()
                .include(CacheBenchmark.class.getSimpleName())
                .forks(0)
                .warmupIterations(2)
                .measurementIterations(3)
                .resultFormat(ResultFormatType.TEXT)
                .build();
        return new Runner(opt).run();
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(null);
    }
}
