package org.example.lab3.drools.service;

import org.example.lab3.domain.Book;
import org.example.lab3.domain.Member;
import org.example.lab3.drools.model.BorrowingDecision;
import org.example.lab3.drools.policy.DroolsBorrowingPolicy;
import org.example.lab3.exception.BorrowingException;
import org.example.lab3.repository.BookRepository;
import org.example.lab3.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Borrowing service powered by the Drools rule engine.
 */
@Service
public class DroolsBorrowingService {

    private static final Logger log = LoggerFactory.getLogger(DroolsBorrowingService.class);

    private final MemberRepository    memberRepository;
    private final BookRepository      bookRepository;
    private final DroolsBorrowingPolicy droolsPolicy;

    public DroolsBorrowingService(MemberRepository memberRepository,
                                  BookRepository bookRepository,
                                  DroolsBorrowingPolicy droolsPolicy) {
        this.memberRepository = memberRepository;
        this.bookRepository   = bookRepository;
        this.droolsPolicy     = droolsPolicy;
    }

    /**
     * Attempts to borrow a book for a member.
     * The Drools engine decides if the borrow is allowed and for how long.
     *
     * @throws BorrowingException if any rule denies the borrow
     */
    public BorrowingDecision borrowBook(String memberId, String bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BorrowingException("Member not found: " + memberId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BorrowingException("Book not found: " + bookId));

        BorrowingDecision decision = droolsPolicy.evaluate(member, book);

        if (!decision.isAllowed()) {
            log.warn("[Drools] DENIED – member='{}' book='{}' reason='{}'",
                    member.getName(), book.getTitle(), decision.getReason());
            throw new BorrowingException(decision.getReason());
        }

        // Apply state changes only after rule approval
        book.setAvailable(false);
        member.borrowBook(bookId);

        log.info("[Drools] APPROVED – member='{}' book='{}' period={} days | policy={}",
                member.getName(), book.getTitle(),
                decision.getBorrowingPeriodDays(), droolsPolicy.policyName());

        return decision;
    }

    /**
     * Returns a book and marks it available again.
     */
    public void returnBook(String memberId, String bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BorrowingException("Member not found: " + memberId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BorrowingException("Book not found: " + bookId));

        member.returnBook(bookId);
        book.setAvailable(true);

        log.info("[Drools] RETURNED – member='{}' book='{}'", member.getName(), book.getTitle());
    }
}
