package org.example.lab3.domain;

/**
 * Roles a library member can have.
 * Used by the role-based borrowing policy.
 */
public enum MemberRole {
    STUDENT,   // max 2 books
    REGULAR,   // max 4 books
    PREMIUM    // max 8 books
}
