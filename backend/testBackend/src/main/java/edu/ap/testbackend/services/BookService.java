package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.dto.googlebooks.GoogleBooksResponse;
import edu.ap.testbackend.dto.googlebooks.VolumeInfo;
import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.repositories.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;

    @Value("${google.books.api.url}")
    private String googleBooksApiUrl;

    public BookService(BookRepository bookRepository, RestTemplate restTemplate) {
        this.bookRepository = bookRepository;
        this.restTemplate = restTemplate;
    }

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public BookDTO addBookByIsbn(String isbn) {
        // Maak de API URL (indien je een API key hebt, voeg deze hier toe aan de string: + "&key=" + jeApiKey)
        String requestUrl = googleBooksApiUrl + isbn;

        // Roep de Google Books API aan
        GoogleBooksResponse response = restTemplate.getForObject(requestUrl, GoogleBooksResponse.class);

        // Controleer of er een boek is gevonden
        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            VolumeInfo volumeInfo = response.getItems().get(0).getVolumeInfo();

            // Maak een nieuwe BookEntity aan op basis van de Google data
            BookEntity newBook = new BookEntity();
            newBook.setTitle(volumeInfo.getTitle() != null ? volumeInfo.getTitle() : "Unknown Title");
            newBook.setAuthors(volumeInfo.getAuthors());
            newBook.setPublisher(volumeInfo.getPublisher());
            newBook.setDescription(volumeInfo.getDescription());
            newBook.setPageCount(volumeInfo.getPageCount());
            newBook.setCategories(volumeInfo.getCategories());
            
            if (volumeInfo.getImageLinks() != null) {
                newBook.setThumbnail(volumeInfo.getImageLinks().getThumbnail());
            }
            
            newBook.setLanguage(volumeInfo.getLanguage());
            newBook.setRating(volumeInfo.getAverageRating());

            // Sla op in de database
            BookEntity savedBook = bookRepository.save(newBook);
            
            // Return de DTO representatie
            return toDTO(savedBook);
        } else {
            throw new IllegalArgumentException("No book found for ISBN: " + isbn);
        }
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