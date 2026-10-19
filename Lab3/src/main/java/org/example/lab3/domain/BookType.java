package org.example.lab3.domain;

/**
 * Classifies a book for use in borrowing-policy rules.
 * STANDARD  – regular shelf book (default)
 * REFERENCE – in-library use only for students; restricted loan period for others
 * RARE      – only PREMIUM members may borrow
 */
public enum BookType {
    STANDARD,
    REFERENCE,
    RARE
}
