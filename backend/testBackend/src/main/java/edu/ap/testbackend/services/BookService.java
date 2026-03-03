package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Haalt alle boeken op uit de database, converteert ze naar BookDTO's en retourneert ze als een lijst
     * @return een lijst van BookDTO's die alle boeken in de database vertegenwoordigen
     */
    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }


    /**
     * Converteert een BookEntity naar een BookDTO
     * @param book de BookEntity die geconverteerd moet worden
     * @return een BookDTO met dezelfde gegevens als de BookEntity
     */
    private BookDTO toDTO(BookEntity book) {
        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthors(),
                book.getPublisher(),
                book.getDescription(),
                book.getPageCount(),
                book.getCategories(),
                book.getThumbnail(),
                book.getLanguage(),
                book.getRating()
        );
    }
}