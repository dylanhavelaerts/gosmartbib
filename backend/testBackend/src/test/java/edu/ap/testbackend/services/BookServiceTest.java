package edu.ap.testbackend.services;

import edu.ap.testbackend.dto.BookDTO;
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
        // Omdat we geen echte Spring Boot context opstarten, vullen we de @Value url
        // handmatig in
        ReflectionTestUtils.setField(bookService, "googleBooksApiUrl",
                "https://www.googleapis.com/books/v1/volumes?q=isbn:");
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
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class)))
                .thenReturn(new GoogleBooksResponse());

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

    @Test
    void givenSpotlightBookExists_whenGetTop4BooksInSpotlight_thenReturnsMappedDTOs() {
        BookEntity book1 = buildBook();
        book1.setId(1L);
        book1.setTitle("Spotlight Book 1");
        book1.setSpotlight(true);

        BookEntity book2 = buildBook();
        book2.setId(2L);
        book2.setTitle("Spotlight Book 2");
        book2.setSpotlight(true);

        BookEntity book3 = buildBook();
        book3.setId(3L);
        book3.setTitle("Not Spotlight Book");
        book3.setSpotlight(false);

        BookEntity book4 = buildBook();
        book4.setId(4L);
        book4.setTitle("Spotlight Book 3");
        book4.setSpotlight(true);

        BookEntity book5 = buildBook();
        book5.setId(5L);
        book5.setTitle("Spotlight Book 4");
        book5.setSpotlight(true);

        when(bookRepository.findTop4BySpotlightTrueOrderByIdDesc()).thenReturn(List.of(book1, book2, book4, book5));

        List<BookDTO> result = bookService.getTop4BooksInSpotlight();

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("Spotlight Book 1", result.get(0).title());
        assertEquals("Spotlight Book 2", result.get(1).title());
        assertEquals("Spotlight Book 3", result.get(2).title());
        assertEquals("Spotlight Book 4", result.get(3).title());
        verify(bookRepository, times(1)).findTop4BySpotlightTrueOrderByIdDesc();
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetTop4BooksInSpotlight_thenReturnsEmptyList() {
        when(bookRepository.findTop4BySpotlightTrueOrderByIdDesc()).thenReturn(List.of());

        List<BookDTO> result = bookService.getTop4BooksInSpotlight();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findTop4BySpotlightTrueOrderByIdDesc();
    }

    @Test
    void givenLatestBooksExist_whenGetLatestBooks_thenReturnsMappedDTOs() {
        BookEntity book1 = buildBook();
        book1.setId(9L);
        book1.setTitle("Old Book");

        BookEntity book2 = buildBook();
        book2.setId(10L);
        book2.setTitle("Newest Book 1");

        BookEntity book3 = buildBook();
        book3.setId(11L);
        book3.setTitle("Newest Book 2");

        BookEntity book4 = buildBook();
        book4.setId(12L);
        book4.setTitle("Newest Book 3");

        BookEntity book5 = buildBook();
        book5.setId(13L);
        book5.setTitle("Newest Book 4");

        when(bookRepository.findTop4ByOrderByIdDesc()).thenReturn(List.of(book2, book3, book4, book5));

        List<BookDTO> result = bookService.getLatestBooks();

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("Newest Book 1", result.get(0).title());
        assertEquals("Newest Book 2", result.get(1).title());
        assertEquals("Newest Book 3", result.get(2).title());
        assertEquals("Newest Book 4", result.get(3).title());
        verify(bookRepository, times(1)).findTop4ByOrderByIdDesc();
    }

    @Test
    void givenNoLatestBooksExist_whenGetLatestBooks_thenReturnsEmptyList() {
        when(bookRepository.findTop4ByOrderByIdDesc()).thenReturn(List.of());

        List<BookDTO> result = bookService.getLatestBooks();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findTop4ByOrderByIdDesc();
    }

    @Test
    void givenBookExists_whenUpdateSpotlight_thenUpdatesSpotlightAndSavesBook() {
        BookEntity book = buildBook();
        book.setSpotlight(false);

        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        bookService.updateSpotlight(10L, true);

        assertTrue(book.isSpotlight());
        verify(bookRepository, times(1)).findById(10L);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenBookDoesNotExist_whenUpdateSpotlight_thenThrowsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.updateSpotlight(99L, true));
        verify(bookRepository, times(1)).findById(99L);
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenSpotlightBooksExist_whenGetAllBooksInSpotlight_thenReturnsMappedDTOs() {
        BookEntity book1 = buildBook();
        book1.setId(1L);
        book1.setTitle("Spotlight Book 1");
        book1.setSpotlight(true);

        BookEntity book2 = buildBook();
        book2.setId(2L);
        book2.setTitle("Spotlight Book 2");
        book2.setSpotlight(true);

        BookEntity book3 = buildBook();
        book3.setId(3L);
        book3.setTitle("Spotlight Book 3");
        book3.setSpotlight(true);

        BookEntity book4 = buildBook();
        book4.setId(4L);
        book4.setTitle("Spotlight Book 4");
        book4.setSpotlight(true);

        BookEntity book5 = buildBook();
        book5.setId(5L);
        book5.setTitle("Spotlight Book 5");
        book5.setSpotlight(true);

        BookEntity book6 = buildBook();
        book6.setId(6L);
        book6.setTitle("Spotlight Book 6");
        book6.setSpotlight(false);

        when(bookRepository.findBySpotlightTrueOrderByIdDesc())
                .thenReturn(List.of(book5, book4, book3, book2, book1));

        List<BookDTO> result = bookService.getAllBooksInSpotlight();

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Spotlight Book 5", result.get(0).title());
        assertEquals("Spotlight Book 4", result.get(1).title());
        assertEquals("Spotlight Book 3", result.get(2).title());
        assertEquals("Spotlight Book 2", result.get(3).title());
        assertEquals("Spotlight Book 1", result.get(4).title());

        verify(bookRepository, times(1)).findBySpotlightTrueOrderByIdDesc();
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetAllBooksInSpotlight_thenReturnsEmptyList() {
        when(bookRepository.findBySpotlightTrueOrderByIdDesc()).thenReturn(List.of());

        List<BookDTO> result = bookService.getAllBooksInSpotlight();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findBySpotlightTrueOrderByIdDesc();
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
                4.7,
                "9780132350884",
                2008);
        book.setId(10L);
        book.setSpotlight(true);
        return book;
    }
}