package org.example.lab3.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reads {@code library.borrowing-policy} and registers the correct
 * {@link BorrowingPolicy} bean so Spring injects it into BorrowingService.
 */
@Configuration
public class BorrowingPolicyConfig {

    private static final Logger log = LoggerFactory.getLogger(BorrowingPolicyConfig.class);

    @Value("${library.borrowing-policy:fixed}")
    private String policyType;

    @Value("${library.fixed-limit:3}")
    private int fixedLimit;

    @Bean
    public BorrowingPolicy borrowingPolicy() {
        BorrowingPolicy policy = switch (policyType.toLowerCase()) {
            case "role-based" -> new RoleBasedBorrowingPolicy();
            default           -> new FixedLimitBorrowingPolicy(fixedLimit);
        };
        log.info("Selected BorrowingPolicy: {}", policy.policyName());
        return policy;
    }
}
