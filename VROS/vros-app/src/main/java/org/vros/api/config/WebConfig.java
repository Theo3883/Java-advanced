package org.vros.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Registers Spring's ShallowEtagHeaderFilter so that all GET responses
 * automatically get an ETag header and conditional GET with If-None-Match
 * returns 304 Not Modified when content hasn't changed.
 */
@Configuration
public class WebConfig {

    @Bean
    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }
}
