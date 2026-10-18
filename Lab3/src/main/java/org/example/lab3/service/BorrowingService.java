package org.example.lab3.service;

import org.example.lab3.domain.Book;
import org.example.lab3.domain.Member;
import org.example.lab3.event.HeavyBorrowerEvent;
import org.example.lab3.exception.BorrowingException;
import org.example.lab3.policy.BorrowingPolicy;
import org.example.lab3.repository.BookRepository;
import org.example.lab3.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service for borrowing and returning books.
 */
@Service
public class BorrowingService {

    private static final Logger log = LoggerFactory.getLogger(BorrowingService.class);

    private final MemberRepository       memberRepository;
    private final BookRepository         bookRepository;
    private final BorrowingPolicy        borrowingPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final int heavyBorrowerThreshold;

    public BorrowingService(MemberRepository memberRepository,
                            BookRepository bookRepository,
                            BorrowingPolicy borrowingPolicy,
                            ApplicationEventPublisher eventPublisher,
                            @Value("${library.heavy-borrower-threshold:2}") int heavyBorrowerThreshold) {
        this.memberRepository        = memberRepository;
        this.bookRepository          = bookRepository;
        this.borrowingPolicy         = borrowingPolicy;
        this.eventPublisher          = eventPublisher;
        this.heavyBorrowerThreshold  = heavyBorrowerThreshold;
    }

    /**
     * Borrows a book for a member.
     * Pre-conditions (member exists, book available, within limit) are validated
     * by {@code BorrowingValidationAspect} before this method executes.
     */
    public void borrowBook(String memberId, String bookId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Book   book   = bookRepository.findById(bookId).orElseThrow();

        if (!borrowingPolicy.canBorrow(member)) {
            throw new BorrowingException("Member '%s' has reached the borrowing limit (%s)."
                    .formatted(member.getName(), borrowingPolicy.policyName()));
        }

        book.setAvailable(false);
        member.borrowBook(bookId);

        log.info("Book '{}' successfully borrowed by '{}'.", book.getTitle(), member.getName());

        // Publish event when the threshold is exceeded
        if (member.getBorrowedBookIds().size() > heavyBorrowerThreshold) {
            eventPublisher.publishEvent(
                    new HeavyBorrowerEvent(this, member, member.getBorrowedBookIds().size()));
        }
    }

    /**
     * Returns a previously borrowed book.
     */
    public void returnBook(String memberId, String bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BorrowingException("Member not found: " + memberId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BorrowingException("Book not found: " + bookId));

        member.returnBook(bookId);
        book.setAvailable(true);

        log.info("Book '{}' returned by '{}'.", book.getTitle(), member.getName());
    }
}
