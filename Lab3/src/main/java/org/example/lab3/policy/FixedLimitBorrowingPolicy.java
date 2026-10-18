package org.example.lab3.policy;

import org.example.lab3.domain.Member;
import org.springframework.beans.factory.annotation.Value;

/**
 * Fixed-limit policy: every member may borrow at most {@code library.fixed-limit} books.
 * Activated via: library.borrowing-policy=fixed
 */
public class FixedLimitBorrowingPolicy implements BorrowingPolicy {

    private final int limit;

    public FixedLimitBorrowingPolicy(@Value("${library.fixed-limit:3}") int limit) {
        this.limit = limit;
    }

    @Override
    public boolean canBorrow(Member member) {
        return member.getBorrowedBookIds().size() < limit;
    }

    @Override
    public String policyName() {
        return "FixedLimit(max=" + limit + ")";
    }
}
