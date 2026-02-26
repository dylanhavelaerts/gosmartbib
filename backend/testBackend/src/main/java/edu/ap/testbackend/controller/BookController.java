package edu.ap.testbackend.controller;

import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BookController {
    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @RequestMapping("/all")
    public List<BookEntity> getBooks(Model model) {
        return bookRepository.findAll();
    }
}
