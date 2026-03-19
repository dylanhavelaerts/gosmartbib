package edu.ap.gosmartlib.exceptions;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(Long id) {
        super("Book with id " + id + " could not be found");
    }
}
