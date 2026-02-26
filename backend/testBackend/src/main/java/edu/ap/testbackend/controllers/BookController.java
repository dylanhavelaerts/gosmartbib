package edu.ap.testbackend.controllers;

import edu.ap.testbackend.services.BookService;
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
    @GetMapping ("get/{id}")
        public BookDTO getBookByid(@PathVariable Long id) throws Exception {
        return bookService.getBookById(id);
        }
    @DeleteMapping("/delete/{id}")
    public void deleteBook(@PathVariable Long id) throws Exception {
        bookService.deleteBook(id);

    }
}
