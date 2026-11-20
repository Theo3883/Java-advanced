package org.vros.worker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bounded executor for the solver tasks.
 *
 * <p>{@link ThreadPoolTaskExecutor} only grows beyond {@code coreSize} once the queue is full.
 * With the default queue of 50, up to 52 tasks can be accepted before a rejection. Set
 * {@code vros.worker.pool.queue-capacity=0} for a strict max-concurrency limit (synchronous
 * queue), which makes rejections visible immediately and exercises the retry path.</p>
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    public static final String OPTIMIZATION_EXECUTOR = "optimizationExecutor";

    private final WorkerProperties properties;

    @Bean(name = OPTIMIZATION_EXECUTOR)
    public ThreadPoolTaskExecutor optimizationExecutor() {
        WorkerProperties.Pool pool = properties.getPool();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(pool.getCoreSize());
        executor.setMaxPoolSize(pool.getMaxSize());
        executor.setQueueCapacity(pool.getQueueCapacity());
        executor.setThreadNamePrefix("vrp-solver-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Optimization executor initialized (core={}, max={}, queue={})",
                pool.getCoreSize(), pool.getMaxSize(), pool.getQueueCapacity());
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("Uncaught async exception in {}", method.getName(), throwable);
    }
}
