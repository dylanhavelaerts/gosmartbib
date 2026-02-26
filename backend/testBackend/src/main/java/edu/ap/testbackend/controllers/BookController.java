package edu.ap.testbackend.controllers;

import edu.ap.testbackend.services.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
@CrossOrigin(origins = "*")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/all")
    public List<BookDTO> getBooks() {
        return bookService.getAllBooks();
    }

    @PostMapping("/add/{isbn}")
    public ResponseEntity<?> addBookByIsbn(@PathVariable String isbn) {
        try {
            BookDTO addedBook = bookService.addBookByIsbn(isbn);
            return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Dit vangt de error op als Google Books geen resultaat heeft
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Dit vangt onverwachte server errors (zoals netwerkproblemen met Google)
            return new ResponseEntity<>("An error occurred while fetching the book.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}