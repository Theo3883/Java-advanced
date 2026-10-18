package org.example.lab3.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.example.lab3.repository.BookRepository;
import org.example.lab3.repository.MemberRepository;

/**
 * Aspect 1 – Logging:
 * Wraps borrowBook / returnBook to log method name, member name,
 * book title, and execution time.
 */
@Aspect
@Component
public class BorrowingLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(BorrowingLoggingAspect.class);

    private final MemberRepository memberRepository;
    private final BookRepository   bookRepository;

    public BorrowingLoggingAspect(MemberRepository memberRepository,
                                  BookRepository   bookRepository) {
        this.memberRepository = memberRepository;
        this.bookRepository   = bookRepository;
    }

    @Around("execution(* org.example.lab3.service.BorrowingService.borrowBook(..)) ||"
          + "execution(* org.example.lab3.service.BorrowingService.returnBook(..))")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        String method   = pjp.getSignature().getName();
        Object[] args   = pjp.getArgs();
        String memberId = (String) args[0];
        String bookId   = (String) args[1];

        String memberName = memberRepository.findById(memberId)
                .map(m -> m.getName()).orElse(memberId);
        String bookTitle  = bookRepository.findById(bookId)
                .map(b -> b.getTitle()).orElse(bookId);

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed  = System.currentTimeMillis() - start;
            log.info("[LOG] method='{}' member='{}' book='{}' time={}ms",
                    method, memberName, bookTitle, elapsed);
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[LOG] method='{}' member='{}' book='{}' time={}ms FAILED",
                    method, memberName, bookTitle, elapsed);
            throw t;
        }
    }
}
