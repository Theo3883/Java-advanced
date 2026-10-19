package org.example.lab3.drools.model;

import org.example.lab3.domain.Book;
import org.example.lab3.domain.BookType;
import org.example.lab3.domain.Member;
import org.example.lab3.domain.MemberRole;

/**
 * Drools fact representing a borrow request.
 *
 * Inserted into the KieSession as the INPUT fact.
 */
public class BorrowingRequest {

    private final String     memberId;
    private final String     memberName;
    private final MemberRole role;
    private final int        borrowedCount;

    private final String   bookId;
    private final String   bookTitle;
    private final BookType bookType;
    private final boolean  bookAvailable;

    public BorrowingRequest(Member member, Book book) {
        this.memberId      = member.getId();
        this.memberName    = member.getName();
        this.role          = member.getRole();
        this.borrowedCount = member.getBorrowedBookIds().size();

        this.bookId        = book.getId();
        this.bookTitle     = book.getTitle();
        this.bookType      = book.getType();
        this.bookAvailable = book.isAvailable();
    }

    //  Getters
    public String     getMemberId()      { return memberId; }
    public String     getMemberName()    { return memberName; }
    public MemberRole getRole()          { return role; }
    public int        getBorrowedCount() { return borrowedCount; }
    public String     getBookId()        { return bookId; }
    public String     getBookTitle()     { return bookTitle; }
    public BookType   getBookType()      { return bookType; }
    public boolean    isBookAvailable()  { return bookAvailable; }

    @Override public String toString() {
        return "BorrowingRequest{member='%s'(%s), borrowed=%d, book='%s'(%s), available=%s}"
                .formatted(memberName, role, borrowedCount, bookTitle, bookType, bookAvailable);
    }
}
