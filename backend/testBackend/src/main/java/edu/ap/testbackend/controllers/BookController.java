package edu.ap.testbackend.controllers;

import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.services.BookService;
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

    @GetMapping("/get/{id}")
    public BookDTO getBookById(@PathVariable Long id) throws BookNotFoundException {
        return bookService.getBookById(id);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) throws BookNotFoundException {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();

    }
}
