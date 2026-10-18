package org.example.lab3.domain;

import java.util.ArrayList;
import java.util.List;

/** Library member. */
public class Member {

    private final String id;
    private final String name;
    private final MemberRole role;
    private final List<String> borrowedBookIds = new ArrayList<>();

    public Member(String id, String name, MemberRole role) {
        this.id   = id;
        this.name = name;
        this.role = role;
    }

    public String       getId()             { return id; }
    public String       getName()           { return name; }
    public MemberRole   getRole()           { return role; }
    public List<String> getBorrowedBookIds(){ return borrowedBookIds; }

    public void borrowBook(String bookId)   { borrowedBookIds.add(bookId); }
    public void returnBook(String bookId)   { borrowedBookIds.remove(bookId); }

    @Override public String toString() {
        return "Member{id='%s', name='%s', role=%s, borrowed=%d}"
                .formatted(id, name, role, borrowedBookIds.size());
    }
}
