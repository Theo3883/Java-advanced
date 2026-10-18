package org.example.lab3.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.example.lab3.domain.Book;
import org.example.lab3.domain.Member;
import org.example.lab3.exception.BorrowingException;
import org.example.lab3.policy.BorrowingPolicy;
import org.example.lab3.repository.BookRepository;
import org.example.lab3.repository.MemberRepository;
import org.springframework.stereotype.Component;

/**
 * Aspect 2 – Validation (Before advice):
 * Before borrowBook executes, check:
 *   1. Member exists
 *   2. Book exists and is available
 *   3. Member has not exceeded the borrowing limit
 * Throws {@link BorrowingException} if any condition fails.
 */
@Aspect
@Component
public class BorrowingValidationAspect {

    private final MemberRepository memberRepository;
    private final BookRepository   bookRepository;
    private final BorrowingPolicy  borrowingPolicy;

    public BorrowingValidationAspect(MemberRepository memberRepository,
                                     BookRepository   bookRepository,
                                     BorrowingPolicy  borrowingPolicy) {
        this.memberRepository = memberRepository;
        this.bookRepository   = bookRepository;
        this.borrowingPolicy  = borrowingPolicy;
    }

    @Before("execution(* org.example.lab3.service.BorrowingService.borrowBook(..))")
    public void validateBorrow(JoinPoint jp) {
        String memberId = (String) jp.getArgs()[0];
        String bookId   = (String) jp.getArgs()[1];

        // 1. Member must exist
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BorrowingException(
                        "Member not found: " + memberId));

        // 2. Book must exist and be available
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BorrowingException(
                        "Book not found: " + bookId));

        if (!book.isAvailable()) {
            throw new BorrowingException(
                    "Book '%s' is not available.".formatted(book.getTitle()));
        }

        // 3. Borrowing limit must not be exceeded
        if (!borrowingPolicy.canBorrow(member)) {
            throw new BorrowingException(
                    "Member '%s' has reached the borrowing limit (%s)."
                            .formatted(member.getName(), borrowingPolicy.policyName()));
        }
    }
}
