package org.example.lab3.event;

import org.example.lab3.domain.Member;
import org.springframework.context.ApplicationEvent;

/** Published when a member has borrowed more books than the configured threshold. */
public class HeavyBorrowerEvent extends ApplicationEvent {

    private final Member member;
    private final int    bookCount;

    public HeavyBorrowerEvent(Object source, Member member, int bookCount) {
        super(source);
        this.member    = member;
        this.bookCount = bookCount;
    }

    public Member getMember()   { return member; }
    public int    getBookCount(){ return bookCount; }
}
