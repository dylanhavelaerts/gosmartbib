package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.dto.googlebooks.GoogleBooksResponse;
import edu.ap.testbackend.dto.googlebooks.VolumeInfo;
import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.repositories.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

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

        BookEntity previewBook = new BookEntity();
        previewBook.setTitle(volumeInfo.getTitle() != null ? volumeInfo.getTitle() : "Onbekende Titel");
        previewBook.setAuthors(volumeInfo.getAuthors() != null ? volumeInfo.getAuthors() : new java.util.ArrayList<>());
        previewBook.setPublisher(volumeInfo.getPublisher());
        previewBook.setDescription(volumeInfo.getDescription());
        previewBook.setPageCount(volumeInfo.getPageCount() != null ? volumeInfo.getPageCount() : 0);
        previewBook.setCategories(
                volumeInfo.getCategories() != null ? volumeInfo.getCategories() : new java.util.ArrayList<>());

        if (volumeInfo.getImageLinks() != null) {
            previewBook.setThumbnail(volumeInfo.getImageLinks().getThumbnail());
        } else {
            previewBook.setThumbnail("");
        }

        previewBook.setLanguage(volumeInfo.getLanguage());
        previewBook.setRating(volumeInfo.getAverageRating() != null ? volumeInfo.getAverageRating() : 0.0);

        previewBook.setIsbn(isbn);
        previewBook.setPublishedYear(extractYear(volumeInfo.getPublishedDate()));

        return toDTO(previewBook);
    }

    public BookDTO addBookByIsbn(String isbn) {
        String requestUrl = googleBooksApiUrl + isbn;
        GoogleBooksResponse response = restTemplate.getForObject(requestUrl, GoogleBooksResponse.class);

        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            VolumeInfo volumeInfo = response.getItems().get(0).getVolumeInfo();

            BookEntity newBook = new BookEntity();
            newBook.setTitle(volumeInfo.getTitle() != null ? volumeInfo.getTitle() : "Onbekende Titel");

            newBook.setAuthors(volumeInfo.getAuthors() != null ? volumeInfo.getAuthors() : new java.util.ArrayList<>());
            newBook.setPublisher(volumeInfo.getPublisher());
            newBook.setDescription(volumeInfo.getDescription());
            newBook.setPageCount(volumeInfo.getPageCount() != null ? volumeInfo.getPageCount() : 0);
            newBook.setCategories(
                    volumeInfo.getCategories() != null ? volumeInfo.getCategories() : new java.util.ArrayList<>());

            if (volumeInfo.getImageLinks() != null) {
                newBook.setThumbnail(volumeInfo.getImageLinks().getThumbnail());
            } else {
                newBook.setThumbnail("");
            }

            newBook.setLanguage(volumeInfo.getLanguage());
            newBook.setRating(volumeInfo.getAverageRating() != null ? volumeInfo.getAverageRating() : 0.0);

            newBook.setIsbn(isbn);
            newBook.setPublishedYear(extractYear(volumeInfo.getPublishedDate()));

            BookEntity savedBook = bookRepository.save(newBook);

            return toDTO(savedBook);
        } else {
            throw new IllegalArgumentException("No book found for ISBN: " + isbn);
        }
    }

    private Integer extractYear(String publishedDate) {
        if (publishedDate != null && publishedDate.length() >= 4) {
            try {
                return Integer.parseInt(publishedDate.substring(0, 4));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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

    public void updateSpotlight(Long id, boolean spotlight) {
        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        book.setSpotlight(spotlight);
        bookRepository.save(book);
    }

    public List<BookDTO> search(String query) {
        if (query == null || query.isBlank()) {
            return bookRepository.findAll()
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }
        return bookRepository.searchByTitleOrAuthor(query.trim())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<BookDTO> getTop4BooksInSpotlight() {
        return bookRepository.findTop4BySpotlightTrueOrderByIdDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<BookDTO> getAllBooksInSpotlight() {
        return bookRepository.findBySpotlightTrueOrderByIdDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<BookDTO> getLatestBooks() {
        return bookRepository.findTop4ByOrderByIdDesc()
                .stream()
                .map(this::toDTO)
                .toList();
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
                book.getRating(),
                book.getIsbn(),
                book.getPublishedYear());
    }

}