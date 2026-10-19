package org.example.lab3.domain;

/** A book in the library. */
public class Book {

    private final String   id;
    private final String   title;
    private final BookType type;
    private boolean        available = true;

    public Book(String id, String title) {
        this(id, title, BookType.STANDARD);
    }

    public Book(String id, String title, BookType type) {
        this.id    = id;
        this.title = title;
        this.type  = type;
    }

    public String   getId()        { return id; }
    public String   getTitle()     { return title; }
    public BookType getType()      { return type; }
    public boolean  isAvailable()  { return available; }

    public void setAvailable(boolean available) { this.available = available; }

    @Override public String toString() {
        return "Book{id='%s', title='%s', type=%s, available=%s}".formatted(id, title, type, available);
    }
}
