package edu.ap.testbackend.controllers;

import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.services.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
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

    @GetMapping("/spotlight")
    public List<BookDTO> getBooksInSpotlight() {
        return bookService.getAllBooksInSpotlight();
    }

    @GetMapping("/latest")
    public List<BookDTO> getLatestBooks() {
        return bookService.getLatestBooks();
    }

}
