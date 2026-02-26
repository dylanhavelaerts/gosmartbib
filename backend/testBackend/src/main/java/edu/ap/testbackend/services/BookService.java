package edu.ap.testbackend.services;

import edu.ap.testbackend.repository.BookRepository;
public class BookService {
    BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }
}