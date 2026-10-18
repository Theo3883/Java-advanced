package org.example.lab3.runner;

import org.example.lab3.domain.Book;
import org.example.lab3.domain.Member;
import org.example.lab3.domain.MemberRole;
import org.example.lab3.exception.BorrowingException;
import org.example.lab3.repository.BookRepository;
import org.example.lab3.repository.MemberRepository;
import org.example.lab3.service.BorrowingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the full demo.
 *
 * Book usage plan (all unique):
 *   b1 → Alice borrows (unavailable for others)
 *   b2 → Bob borrows (1st)
 *   b3 → Bob borrows (2nd)
 *   b4 → Alice borrows 2nd (reaches STUDENT cap of 2)
 *   b5 → Bob borrows (3rd → triggers HeavyBorrowerEvent since 3 > threshold 2)
 *   b6 → free book used for error scenarios and post-return borrow
 */
@Component
public class LibraryDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LibraryDemoRunner.class);

    private final MemberRepository memberRepository;
    private final BookRepository   bookRepository;
    private final BorrowingService borrowingService;

    public LibraryDemoRunner(MemberRepository memberRepository,
                             BookRepository   bookRepository,
                             BorrowingService borrowingService) {
        this.memberRepository = memberRepository;
        this.bookRepository   = bookRepository;
        this.borrowingService = borrowingService;
    }

    @Override
    public void run(String... args) {
        log.info("=== Library Demo Start ===");

        // ── Seed members 
        memberRepository.save(new Member("m1", "Alice", MemberRole.STUDENT));  // role limit: 2
        memberRepository.save(new Member("m2", "Bob",   MemberRole.PREMIUM));  // role limit: 8

        // ── Seed books 
        bookRepository.save(new Book("b1", "Clean Code"));
        bookRepository.save(new Book("b2", "Design Patterns"));
        bookRepository.save(new Book("b3", "Effective Java"));
        bookRepository.save(new Book("b4", "Refactoring"));
        bookRepository.save(new Book("b5", "SICP"));
        bookRepository.save(new Book("b6", "The Pragmatic Programmer"));

        // ── [1] Happy path
        log.info("--- [1] Happy path ---");
        borrowingService.borrowBook("m1", "b1");  // Alice: 1 book
        borrowingService.borrowBook("m2", "b2");  // Bob: 1 book
        borrowingService.borrowBook("m2", "b3");  // Bob: 2 books
        borrowingService.borrowBook("m2", "b5");  // Bob: 3 books → 3 > threshold(2) → HeavyBorrowerEvent!

        // ── [2] Error: member not found 
        log.info("--- [2] Error: member not found ---");
        tryBorrow("m99", "b6");  // unknown member → BorrowingException from validation aspect

        // ── [3] Error: book not available
        log.info("--- [3] Error: book not available ---");
        tryBorrow("m2", "b1");   // b1 is already taken by Alice → BorrowingException

        // ── [4] Error: borrowing limit exceeded 
        log.info("--- [4] Error: borrowing limit exceeded ---");
        borrowingService.borrowBook("m1", "b4");  // Alice: 2 books → exactly at STUDENT cap
        tryBorrow("m1", "b6");                    // Alice wants a 3rd → denied by policy

        // ── [5] Return and re-borrow 
        log.info("--- [5] Return and re-borrow ---");
        borrowingService.returnBook("m1", "b1");  // Alice: back to 1 book (b4 remains)
        borrowingService.borrowBook("m1", "b6");  // Alice: 2 books again (b4 + b6) → OK

        log.info("=== Library Demo End ===");
    }

    private void tryBorrow(String memberId, String bookId) {
        try {
            borrowingService.borrowBook(memberId, bookId);
        } catch (BorrowingException e) {
            log.warn("Expected error caught: {}", e.getMessage());
        }
    }
}
