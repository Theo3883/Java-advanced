package org.example.lab3.domain;

/** A book in the library. */
public class Book {

    private final String id;
    private final String title;
    private boolean available = true;

    public Book(String id, String title) {
        this.id    = id;
        this.title = title;
    }

    public String  getId()        { return id; }
    public String  getTitle()     { return title; }
    public boolean isAvailable()  { return available; }

    public void setAvailable(boolean available) { this.available = available; }

    @Override public String toString() {
        return "Book{id='%s', title='%s', available=%s}".formatted(id, title, available);
    }
}
