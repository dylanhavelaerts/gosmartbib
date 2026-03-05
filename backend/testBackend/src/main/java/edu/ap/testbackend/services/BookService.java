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

    public BookDTO searchBookByIsbn(String isbn) {
        String url = googleBooksApiUrl + isbn;
        GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalArgumentException("No book found for ISBN: " + isbn);
        }

        VolumeInfo volumeInfo = response.getItems().get(0).getVolumeInfo();

        // Maak een tijdelijke BookEntity om de data netjes te structureren (maar we slaan hem NIET op)
        BookEntity previewBook = new BookEntity();
        previewBook.setTitle(volumeInfo.getTitle() != null ? volumeInfo.getTitle() : "Onbekende Titel");
        previewBook.setAuthors(volumeInfo.getAuthors() != null ? volumeInfo.getAuthors() : new java.util.ArrayList<>());
        previewBook.setPublisher(volumeInfo.getPublisher());
        previewBook.setDescription(volumeInfo.getDescription());
        previewBook.setPageCount(volumeInfo.getPageCount() != null ? volumeInfo.getPageCount() : 0);
        previewBook.setCategories(volumeInfo.getCategories() != null ? volumeInfo.getCategories() : new java.util.ArrayList<>());
        
        if (volumeInfo.getImageLinks() != null) {
            previewBook.setThumbnail(volumeInfo.getImageLinks().getThumbnail());
        } else {
            previewBook.setThumbnail("");
        }
        
        previewBook.setLanguage(volumeInfo.getLanguage());
        previewBook.setRating(volumeInfo.getAverageRating() != null ? volumeInfo.getAverageRating() : 0.0);

        // Stuur de data als DTO terug naar de frontend ter controle
        return toDTO(previewBook);
    }

    public BookDTO addBookByIsbn(String isbn) {
        String requestUrl = googleBooksApiUrl + isbn;
        GoogleBooksResponse response = restTemplate.getForObject(requestUrl, GoogleBooksResponse.class);

        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            VolumeInfo volumeInfo = response.getItems().get(0).getVolumeInfo();

            BookEntity newBook = new BookEntity();
            newBook.setTitle(volumeInfo.getTitle() != null ? volumeInfo.getTitle() : "Onbekende Titel");
            
            // Veilige checks toevoegen, net als in de search functie!
            newBook.setAuthors(volumeInfo.getAuthors() != null ? volumeInfo.getAuthors() : new java.util.ArrayList<>());
            newBook.setPublisher(volumeInfo.getPublisher());
            newBook.setDescription(volumeInfo.getDescription());
            newBook.setPageCount(volumeInfo.getPageCount() != null ? volumeInfo.getPageCount() : 0);
            newBook.setCategories(volumeInfo.getCategories() != null ? volumeInfo.getCategories() : new java.util.ArrayList<>());
            
            if (volumeInfo.getImageLinks() != null) {
                newBook.setThumbnail(volumeInfo.getImageLinks().getThumbnail());
            } else {
                newBook.setThumbnail("");
            }
            
            newBook.setLanguage(volumeInfo.getLanguage());
            newBook.setRating(volumeInfo.getAverageRating() != null ? volumeInfo.getAverageRating() : 0.0);

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