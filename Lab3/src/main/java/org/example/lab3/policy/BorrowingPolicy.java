package org.example.lab3.policy;

import org.example.lab3.domain.Member;

/**
 * Strategy interface for borrowing policies.
 * Each implementation defines the max books a member may borrow.
 */
public interface BorrowingPolicy {
    /**
     * Returns true if the member is allowed to borrow one more book.
     * @param member the requesting member
     */
    boolean canBorrow(Member member);

    /** Human-readable name for logging/debugging. */
    String policyName();
}
