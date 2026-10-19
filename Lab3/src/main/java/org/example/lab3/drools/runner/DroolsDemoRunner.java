package org.example.lab3.drools.runner;

import org.example.lab3.domain.Book;
import org.example.lab3.domain.BookType;
import org.example.lab3.domain.Member;
import org.example.lab3.domain.MemberRole;
import org.example.lab3.drools.model.BorrowingDecision;
import org.example.lab3.drools.service.DroolsBorrowingService;
import org.example.lab3.exception.BorrowingException;
import org.example.lab3.repository.BookRepository;
import org.example.lab3.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Drools demo runner – runs after the original LibraryDemoRunner (@Order(2)).
 *
 * Demonstrates all key Drools rule paths:
 *
 *   [1] Happy path borrow – rules approve and assign borrowing periods
 *   [2] Book type restriction – STUDENT denied REFERENCE / RARE books
 *   [3] Borrowing limit – rules deny once per-role limit is reached
 *   [4] Availability – rule denies when book is already borrowed
 *   [5] Return – book becomes available again
 *
 * Members and books use "d" prefix IDs so they don't collide with
 * the original LibraryDemoRunner's data.
 */
@Component
@Order(2)
public class DroolsDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DroolsDemoRunner.class);

    private final MemberRepository      memberRepository;
    private final BookRepository        bookRepository;
    private final DroolsBorrowingService droolsService;

    public DroolsDemoRunner(MemberRepository memberRepository,
                            BookRepository bookRepository,
                            DroolsBorrowingService droolsService) {
        this.memberRepository = memberRepository;
        this.bookRepository   = bookRepository;
        this.droolsService    = droolsService;
    }

    @Override
    public void run(String... args) {
        log.info("------DROOLS RULE ENGINE DEMO – START -------");

        // ── Seed members ──────────────────────────────────────────────────────
        memberRepository.save(new Member("dm1", "Diana",   MemberRole.STUDENT));   // cap: 2
        memberRepository.save(new Member("dm2", "Edward",  MemberRole.REGULAR));   // cap: 4
        memberRepository.save(new Member("dm3", "Fiona",   MemberRole.PREMIUM));   // cap: 8

        // ── Seed books (mix of types) ─────────────────────────────────────────
        bookRepository.save(new Book("db1", "Head First Java",           BookType.STANDARD));
        bookRepository.save(new Book("db2", "Oxford English Grammar",    BookType.REFERENCE));
        bookRepository.save(new Book("db3", "First Folio (1623)",        BookType.RARE));
        bookRepository.save(new Book("db4", "Spring in Action",          BookType.STANDARD));
        bookRepository.save(new Book("db5", "Encyclopedia Britannica",   BookType.REFERENCE));
        bookRepository.save(new Book("db6", "Kotlin in Action",          BookType.STANDARD));
        bookRepository.save(new Book("db7", "Clean Architecture",        BookType.STANDARD));
        bookRepository.save(new Book("db8", "Webster Unabridged",        BookType.REFERENCE));
        bookRepository.save(new Book("db9", "Magna Carta Facsimile",     BookType.RARE));

        // ── [1] Happy path – rules decide period per role+type ────────────────
        log.info("--- [1] Happy path (approved with borrowing periods) ---");
        borrow("dm1", "db1");  // STUDENT + STANDARD   → approved (14 days)
        borrow("dm2", "db4");  // REGULAR + STANDARD   → approved (21 days)
        borrow("dm2", "db2");  // REGULAR + REFERENCE  → approved (10 days)
        borrow("dm3", "db3");  // PREMIUM + RARE       → approved (7 days)
        borrow("dm3", "db5");  // PREMIUM + REFERENCE  → approved (21 days)

        // ── [2] Book-type restrictions ────────────────────────────────────────
        log.info("--- [2] Book-type restrictions (rule denials) ---");
        tryBorrow("dm1", "db8");   // STUDENT + available REFERENCE book -> denied by type rule
        tryBorrow("dm1", "db9");   // STUDENT + available RARE book      -> denied by type rule
        tryBorrow("dm2", "db9");   // REGULAR + available RARE book      -> denied by type rule

        // ── [3] Borrowing limit ───────────────────────────────────────────────
        log.info("--- [3] Borrowing limit exceeded (rule denial) ---");
        // Diana (STUDENT) currently has db1 (1 book). Borrow db6 to reach cap of 2:
        borrow("dm1", "db6");      // STUDENT + STANDARD -> approved (now holds 2/2)
        tryBorrow("dm1", "db7");   // 3rd attempt on free STANDARD book -> denied by limit rule (max 2)

        // ── [4] Availability check ────────────────────────────────────────────
        log.info("--- [4] Book not available (rule denial) ---");
        tryBorrow("dm3", "db4");   // db4 already borrowed by Edward -> denied by availability rule

        // ── [5] Return and re-borrow ──────────────────────────────────────────
        log.info("--- [5] Return and re-borrow ---");
        droolsService.returnBook("dm1", "db1");  // Diana returns db1 (now holds 1/2)
        borrow("dm1", "db7");                    // STUDENT borrows db7 -> approved (14 days)

        log.info("------- DROOLS RULE ENGINE DEMO – END -----");
    }

    /** Attempts a borrow and logs the decision details on success. */
    private void borrow(String memberId, String bookId) {
        try {
            droolsService.borrowBook(memberId, bookId);
            // decision details already logged inside DroolsBorrowingService
        } catch (BorrowingException e) {
            log.error("[Drools] Unexpected denial: {}", e.getMessage());
        }
    }

    /** Attempts a borrow expected to fail; logs the denial reason. */
    private void tryBorrow(String memberId, String bookId) {
        try {
            droolsService.borrowBook(memberId, bookId);
            log.error("[Drools] Expected denial but borrow succeeded for member={} book={}", memberId, bookId);
        } catch (BorrowingException e) {
            log.info("[Drools] Correctly denied: '{}'", e.getMessage());
        }
    }
}
