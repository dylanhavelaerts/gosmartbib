package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.dto.googlebooks.GoogleBookItem;
import edu.ap.testbackend.dto.googlebooks.GoogleBooksResponse;
import edu.ap.testbackend.dto.googlebooks.VolumeInfo;
import edu.ap.testbackend.entities.BookEntity;
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

    // --- Hulpmethode om snel Google Books responses te faken ---
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
}