package org.example.lab3.policy;

import org.example.lab3.domain.Member;

import java.util.Map;

/**
 * Role-based policy: max books depends on the member's role.
 * STUDENT → 2, REGULAR → 4, PREMIUM → 8
 * Activated via: library.borrowing-policy=role-based
 */
public class RoleBasedBorrowingPolicy implements BorrowingPolicy {

    private static final Map<org.example.lab3.domain.MemberRole, Integer> LIMITS = Map.of(
            org.example.lab3.domain.MemberRole.STUDENT, 2,
            org.example.lab3.domain.MemberRole.REGULAR, 4,
            org.example.lab3.domain.MemberRole.PREMIUM, 8
    );

    @Override
    public boolean canBorrow(Member member) {
        int limit = LIMITS.getOrDefault(member.getRole(), 2);
        return member.getBorrowedBookIds().size() < limit;
    }

    @Override
    public String policyName() {
        return "RoleBased(STUDENT=2, REGULAR=4, PREMIUM=8)";
    }
}
