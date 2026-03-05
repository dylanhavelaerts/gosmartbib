package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.dto.googlebooks.GoogleBookItem;
import edu.ap.testbackend.dto.googlebooks.GoogleBooksResponse;
import edu.ap.testbackend.dto.googlebooks.VolumeInfo;
import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.repositories.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository; // We faken de database

    @Mock
    private RestTemplate restTemplate; // We faken de Google API

    @InjectMocks
    private BookService bookService; // Dit is de service die we écht testen

    @BeforeEach
    void setUp() {
        // Omdat we geen echte Spring Boot context opstarten, vullen we de @Value url handmatig in
        ReflectionTestUtils.setField(bookService, "googleBooksApiUrl", "https://www.googleapis.com/books/v1/volumes?q=isbn:");
    }

    // --- Google API Tests ---

    @Test
    void searchBookByIsbn_ShouldReturnPreview_WhenBookIsFound() {
        // 1. Arrange (Zet de fakedata klaar)
        String isbn = "9798988421504";
        GoogleBooksResponse mockResponse = createMockGoogleResponse("Test Boek", "Test Auteur");
        
        // Vertel de neppe RestTemplate wat hij moet teruggeven
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(mockResponse);

        // 2. Act (Voer de methode uit)
        BookDTO result = bookService.searchBookByIsbn(isbn);

        // 3. Assert (Controleer of het klopt)
        assertNotNull(result);
        assertEquals("Test Boek", result.title());
        
        // Heel belangrijk: bij zoeken mogen we NOOIT iets opslaan in de database!
        verify(bookRepository, never()).save(any(BookEntity.class)); 
    }

    @Test
    void addBookByIsbn_ShouldSaveToDatabase_WhenBookIsFound() {
        // 1. Arrange
        String isbn = "9798988421504";
        GoogleBooksResponse mockResponse = createMockGoogleResponse("Gekocht Boek", "Auteur X");
        
        BookEntity mockSavedEntity = new BookEntity();
        mockSavedEntity.setId(1L);
        mockSavedEntity.setTitle("Gekocht Boek");

        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(mockResponse);
        // Vertel de neppe database wat hij moet doen als er save() wordt aangeroepen
        when(bookRepository.save(any(BookEntity.class))).thenReturn(mockSavedEntity);

        // 2. Act
        BookDTO result = bookService.addBookByIsbn(isbn);

        // 3. Assert
        assertNotNull(result);
        assertEquals(1L, result.id());
        
        // Heel belangrijk: controleer of save() daadwerkelijk is aangeroepen
        verify(bookRepository, times(1)).save(any(BookEntity.class));
    }

    @Test
    void searchBookByIsbn_ShouldThrowException_WhenNoBookFound() {
        // 1. Arrange (Google geeft een leeg resultaat terug)
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(new GoogleBooksResponse());

        // 2 & 3. Act & Assert (Controleer of de verwachte foutmelding wordt gegooid)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookService.searchBookByIsbn("9798988421507");
        });

        assertTrue(exception.getMessage().contains("No book found for ISBN"));
        verify(bookRepository, never()).save(any());
    }

    // --- getAllBooks Tests ---

    @Test
    void givenOneBookExists_whenGetAllBooks_thenReturnsCorrectDTOMapping() {
        when(bookRepository.findAll()).thenReturn(List.of(buildBook()));

        List<BookDTO> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        BookDTO dto = result.get(0);
        assertEquals(10L, dto.id());
        assertEquals("Clean Code", dto.title());
        assertEquals(List.of("Robert C. Martin"), dto.authors());
        assertEquals("Prentice Hall", dto.publisher());
        assertEquals(464, dto.pageCount());
        assertEquals(List.of("Programming", "Software Engineering"), dto.categories());
        assertEquals("https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg", dto.thumbnail());
        assertEquals("en", dto.language());
        assertEquals(4.7, dto.rating());

        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenNoBooksExist_whenGetAllBooks_thenReturnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<BookDTO> result = bookService.getAllBooks();

        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenMultipleBooksExist_whenGetAllBooks_thenReturnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(buildBook(), buildBook()));

        List<BookDTO> result = bookService.getAllBooks();

        assertEquals(2, result.size());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenRepositoryFails_whenGetAllBooks_thenThrowsException() {
        when(bookRepository.findAll()).thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class, () -> bookService.getAllBooks());
    }

    // --- getBookById Tests ---

    @Test
    void givenBookExists_whenGetBookById_thenReturnsCorrectDTO() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(buildBook()));

        BookDTO result = bookService.getBookById(10L);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Clean Code", result.title());
        verify(bookRepository, times(1)).findById(10L);
    }

    @Test
    void givenBookDoesNotExist_whenGetBookById_thenThrowsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(99L));
        verify(bookRepository, times(1)).findById(99L);
    }

    // --- deleteBook Tests ---

    @Test
    void givenBookExists_whenDeleteBook_thenRepositoryDeleteIsCalled() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        bookService.deleteBook(10L);

        verify(bookRepository, times(1)).delete(book);
    }

    @Test
    void givenBookDoesNotExist_whenDeleteBook_thenThrowsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.deleteBook(99L));
        verify(bookRepository, never()).delete(any(BookEntity.class));
    }

    // --- Hulpmethoden (Helper Methods) ---

    private GoogleBooksResponse createMockGoogleResponse(String title, String author) {
        GoogleBooksResponse response = new GoogleBooksResponse();
        GoogleBookItem item = new GoogleBookItem();
        VolumeInfo volumeInfo = new VolumeInfo();
        
        volumeInfo.setTitle(title);
        List<String> authors = new ArrayList<>();
        authors.add(author);
        volumeInfo.setAuthors(authors);
        volumeInfo.setPageCount(300);
        
        item.setVolumeInfo(volumeInfo);
        
        List<GoogleBookItem> items = new ArrayList<>();
        items.add(item);
        response.setItems(items);
        
        return response;
    }

    private BookEntity buildBook() {
        BookEntity book = new BookEntity(
                "Clean Code",
                List.of("Robert C. Martin"),
                "Prentice Hall",
                "A handbook of agile software craftsmanship.",
                464,
                List.of("Programming", "Software Engineering"),
                "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg",
                "en",
                4.7
        );
        book.setId(10L);
        return book;
    }
}