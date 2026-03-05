package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Zoekt een boek op via het id in de database.
     * Als het gevonden wordt, wordt het omgezet naar een DTO en teruggegeven.
     * Als het niet gevonden wordt, gooit het een BookNotFoundException met het id.
     */
    public BookDTO getBookById(Long id) throws BookNotFoundException {
        return bookRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BookNotFoundException(id));
    }
    /**
     * Zoekt een boek op via het id.
     * Als het niet gevonden wordt, gooit het een BookNotFoundException.
     * Als het gevonden wordt, wordt het verwijderd uit de database.
     */
    public void deleteBook(Long id) throws BookNotFoundException {
        BookEntity book = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));

        bookRepository.delete(book);

    }


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