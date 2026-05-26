package edu.ap.gosmartlib.util;

import edu.ap.gosmartlib.entities.book.BookEntity;

import java.util.List;

public class BookDisplayUtil {

    private BookDisplayUtil() {}

    public static String resolveTitle(BookEntity book, String isbn) {
        return book != null
                ? book.getTitle()
                : "Onbekend Boek (ISBN: " + isbn + ")";
    }

    public static String resolveAuthor(BookEntity book) {
        if (book == null) return "Onbekende Auteur";
        List<String> authors = book.getAuthors();
        return (authors != null && !authors.isEmpty())
                ? String.join(", ", authors)
                : "Onbekende Auteur";
    }
}