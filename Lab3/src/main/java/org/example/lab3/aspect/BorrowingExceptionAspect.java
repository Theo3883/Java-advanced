package org.example.lab3.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect 3 – Exception interception:
 * Catches any exception thrown by BorrowingService operations
 * and logs the failed operation name + error message.
 */
@Aspect
@Component
public class BorrowingExceptionAspect {

    private static final Logger log = LoggerFactory.getLogger(BorrowingExceptionAspect.class);

    @AfterThrowing(
        pointcut = "execution(* org.example.lab3.service.BorrowingService.*(..))",
        throwing  = "ex"
    )
    public void handleException(JoinPoint jp, Exception ex) {
        String operation = jp.getSignature().toShortString();
        log.error("[EXCEPTION] Operation='{}' failed — {}", operation, ex.getMessage());
    }
}
