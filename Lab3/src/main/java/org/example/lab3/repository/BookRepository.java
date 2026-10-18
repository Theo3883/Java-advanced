package org.example.lab3.repository;

import org.example.lab3.domain.Book;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Simple in-memory book store. */
@Repository
public class BookRepository {

    private final Map<String, Book> store = new HashMap<>();

    public void save(Book book)                    { store.put(book.getId(), book); }
    public Optional<Book> findById(String id)      { return Optional.ofNullable(store.get(id)); }
    public boolean existsById(String id)           { return store.containsKey(id); }
}
