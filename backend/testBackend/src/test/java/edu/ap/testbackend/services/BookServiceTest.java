package edu.ap.testbackend.services;

import edu.ap.testbackend.dto.BookDTO;
import edu.ap.testbackend.dto.CreateBookRequestDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import edu.ap.testbackend.dto.importdto.BulkImportResponseDTO;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
                "https://www.googleapis.com/books/v1/volumes");
        ReflectionTestUtils.setField(bookService, "googleBooksApiKey", "test-key");
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

        assertEquals("Geen boek voor ISBN: 9798988421507", exception.getMessage());
        verify(bookRepository, never()).save(any());
    }

    // --- getAllBooks Tests ---

    @Test
    void givenOneBookExists_whenGetAllBooks_thenReturnsCorrectDTOMapping() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(toEntityPage(List.of(buildBook())));

        Page<BookDTO> result = bookService.getAllBooks(0, 20);

        assertEquals(1, result.getTotalElements());
        BookDTO dto = result.getContent().get(0);
        assertEquals(10L, dto.id());
        assertEquals("Clean Code", dto.title());
        assertEquals(List.of("Robert C. Martin"), dto.authors());
        assertEquals("Prentice Hall", dto.publisher());
        assertEquals(464, dto.pageCount());
        assertEquals(List.of("Programming", "Software Engineering"), dto.categories());
        assertEquals("https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg", dto.thumbnail());
        assertEquals("en", dto.language());
        assertEquals(4.7, dto.rating());

        verify(bookRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void givenNoBooksExist_whenGetAllBooks_thenReturnsEmptyPage() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(toEntityPage(List.of()));

        Page<BookDTO> result = bookService.getAllBooks(0, 20);

        assertEquals(0, result.getTotalElements());
        verify(bookRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void givenMultipleBooksExist_whenGetAllBooks_thenReturnsAllBooks() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(toEntityPage(List.of(buildBook(), buildBook())));

        Page<BookDTO> result = bookService.getAllBooks(0, 20);

        assertEquals(2, result.getTotalElements());
        verify(bookRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void givenRepositoryFails_whenGetAllBooks_thenThrowsException() {
        when(bookRepository.findAll(any(Pageable.class))).thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class, () -> bookService.getAllBooks(0, 20));
    }

    // tests for invalid page/size
    @Test
    void givenNegativePage_whenGetAllBooks_thenThrowsException() {
        assertThrows(RuntimeException.class, () -> bookService.getAllBooks(-1, 20));
        verify(bookRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void givenZeroSize_whenGetAllBooks_thenThrowsException() {
        assertThrows(RuntimeException.class, () -> bookService.getAllBooks(0, 0));
        verify(bookRepository, never()).findAll(any(Pageable.class));
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

    // --- searchByTitleOrAuthor Tests ---

    @Test
    void givenNullQuery_whenSearchByTitleOrAuthor_thenReturnsAllBooks() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(toEntityPage(List.of(buildBook())));

        Page<BookDTO> result = bookService.searchByTitleOrAuthor(null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Clean Code", result.getContent().get(0).title());
        verify(bookRepository, times(1)).findAll(any(Pageable.class));
        verify(bookRepository, never()).searchByTitleOrAuthor(anyString(), any(Pageable.class));
    }

    @Test
    void givenBlankQuery_whenSearchByTitleOrAuthor_thenReturnsAllBooks() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(toEntityPage(List.of(buildBook())));

        Page<BookDTO> result = bookService.searchByTitleOrAuthor("   ", 0, 20);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findAll(any(Pageable.class));
        verify(bookRepository, never()).searchByTitleOrAuthor(anyString(), any(Pageable.class));
    }

    @Test
    void givenValidQuery_whenSearchByTitleOrAuthor_thenReturnsMatchingBooks() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.searchByTitleOrAuthor(eq("Clean"), any(Pageable.class))).thenReturn(entityPage);

        Page<BookDTO> result = bookService.searchByTitleOrAuthor("Clean", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Clean Code", result.getContent().get(0).title());
        verify(bookRepository, times(1)).searchByTitleOrAuthor(eq("Clean"), any(Pageable.class));
        verify(bookRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void givenQueryWithWhitespace_whenSearchByTitleOrAuthor_thenTrimsAndSearches() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.searchByTitleOrAuthor(eq("Clean"), any(Pageable.class))).thenReturn(entityPage);

        Page<BookDTO> result = bookService.searchByTitleOrAuthor("  Clean  ", 0, 20);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).searchByTitleOrAuthor(eq("Clean"), any(Pageable.class));
    }

    @Test
    void givenNoMatchingBooks_whenSearchByTitleOrAuthor_thenReturnsEmptyPage() {
        Page<BookEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(bookRepository.searchByTitleOrAuthor(eq("Nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

        Page<BookDTO> result = bookService.searchByTitleOrAuthor("Nonexistent", 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(bookRepository, times(1)).searchByTitleOrAuthor(eq("Nonexistent"), any(Pageable.class));
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
                2008, true, List.of("Toekomst & technologie"), "A", 1, 1);
        book.setId(10L);
        book.setSpotlight(true);
        return book;
    }
    // --- filterBooks Service Tests ---

    @Test
    void givenValidFilters_whenFilterBooks_thenReturnsMappedDTOs() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.filterBooks(eq("en"), eq(List.of("Programming")), eq(100), eq(500), eq(2000), eq(2023),
                any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.filterBooks("en", List.of("Programming"), 100, 500, 2000, 2023, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Clean Code", result.getContent().get(0).title());
        verify(bookRepository, times(1)).filterBooks(eq("en"), eq(List.of("Programming")), eq(100), eq(500), eq(2000),
                eq(2023), any(Pageable.class));
    }

    @Test
    void givenNullFilters_whenFilterBooks_thenReturnsAllBooks() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook(), buildBook()), PageRequest.of(0, 20), 2);
        when(bookRepository.filterBooks(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.filterBooks(null, null, null, null, null, null, 0, 20);

        assertEquals(2, result.getTotalElements());
        verify(bookRepository, times(1)).filterBooks(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class));
    }

    @Test
    void givenNoMatchingBooks_whenFilterBooks_thenReturnsEmptyPage() {
        Page<BookEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(bookRepository.filterBooks(eq("nl"), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<BookDTO> result = bookService.filterBooks("nl", null, null, null, null, null, 0, 20);

        assertEquals(0, result.getTotalElements());
        verify(bookRepository, times(1)).filterBooks(eq("nl"), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class));
    }

    @Test
    void givenMinPageCountGreaterThanMaxPageCount_whenFilterBooks_thenThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookService.filterBooks(null, null, 500, 100, null, null, 0, 20));

        verify(bookRepository, never()).filterBooks(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void givenMinYearGreaterThanMaxYear_whenFilterBooks_thenThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookService.filterBooks(null, null, null, null, 2023, 2000, 0, 20));

        verify(bookRepository, never()).filterBooks(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void givenRepositoryFails_whenFilterBooks_thenThrowsException() {
        when(bookRepository.filterBooks(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class,
                () -> bookService.filterBooks(null, null, null, null, null, null, 0, 20));
    }

    @Test
    void givenEmptyExcelFile_whenImportBooksFromExcel_thenThrowsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.importBooksFromExcel(file));

        assertEquals("Upload een excel file die niet leeg is", ex.getMessage());
        verifyNoInteractions(bookRepository, restTemplate);
    }

    @Test
    void givenValidExcelRow_whenImportBooksFromExcel_thenSavesBookAndReturnsCorrectSummary() throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "Clean Code" }
        });

        GoogleBooksResponse googleResponse = createMockGoogleResponse("Clean Code", "Robert C. Martin");

        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(googleResponse);
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file);

        assertEquals(1, result.totalRows());
        assertEquals(1, result.savedCount());
        assertEquals(0, result.mismatchCount());
        assertTrue(result.mismatches().isEmpty());

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository, times(1)).save(captor.capture());

        BookEntity savedBook = captor.getValue();
        assertEquals("Clean Code", savedBook.getTitle());
        assertEquals("9780132350884", savedBook.getIsbn());
    }

    @Test
    void givenBookAlreadyExistsInDatabase_whenImportBooksFromExcel_thenAddsMismatchAndDoesNotSave() throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "Clean Code" }
        });

        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file);

        assertEquals(1, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(1, result.mismatchCount());
        assertEquals(1, result.mismatches().size());

        verify(bookRepository, never()).save(any(BookEntity.class));
        verify(restTemplate, never()).getForObject(anyString(), eq(GoogleBooksResponse.class));
    }

    @Test
    void givenExcelTitleDoesNotMatchGoogleTitle_whenImportBooksFromExcel_thenAddsMismatchAndDoesNotSave()
            throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "Clean Code" }
        });

        GoogleBooksResponse googleResponse = createMockGoogleResponse("Refactoring", "Martin Fowler");

        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(googleResponse);

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file);

        assertEquals(1, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(1, result.mismatchCount());
        assertEquals(1, result.mismatches().size());

        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenGoogleBooksReturnsNoResults_whenImportBooksFromExcel_thenAddsMismatchAndDoesNotSave() throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "Clean Code" }
        });

        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class)))
                .thenReturn(new GoogleBooksResponse());

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file);

        assertEquals(1, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(1, result.mismatchCount());
        assertEquals(1, result.mismatches().size());

        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenRowWithMissingIsbnOrTitle_whenImportBooksFromExcel_thenAddsMismatchAndDoesNotCallGoogle()
            throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "" },
                { "", "Clean Code" }
        });

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file);

        assertEquals(2, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(2, result.mismatchCount());
        assertEquals(2, result.mismatches().size());

        verify(bookRepository, never()).save(any(BookEntity.class));
        verify(restTemplate, never()).getForObject(anyString(), eq(GoogleBooksResponse.class));
    }

    @Test
    void givenUnreadableExcelFile_whenImportBooksFromExcel_thenThrowsRuntimeException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> bookService.importBooksFromExcel(file));

        assertEquals("Kon de excel file niet lezen", ex.getMessage());
    }
    // --- updateBook Service Tests ---

    @Test
    void givenBookExists_whenUpdateBook_thenUpdatesFieldsAndReturnsMappedDTO() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO update = new BookDTO(null, "Refactoring", List.of("Martin Fowler"), null, null,
                null, null, null, null, null, null, null, false, null, null, null, null);

        BookDTO result = bookService.updateBook(10L, update);

        assertEquals("Refactoring", result.title());
        assertEquals(List.of("Martin Fowler"), result.authors());
        assertEquals("Prentice Hall", result.publisher()); // unchanged
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenBookDoesNotExist_whenUpdateBook_thenThrowsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        BookDTO update = new BookDTO(null, "New Title", null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null);

        assertThrows(BookNotFoundException.class, () -> bookService.updateBook(99L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenBlankTitle_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, "   ", null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenEmptyTitle_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, "", null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenNegativePageCount_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, null, null, null, null,
                -1, null, null, null, null, null, null, false, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenFuturePublishedYear_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        int futureYear = Year.now().getValue() + 1;
        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, futureYear, false, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenCurrentYear_whenUpdateBook_thenSavesSuccessfully() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int currentYear = Year.now().getValue();
        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, currentYear, false, null, null, null, null);

        assertDoesNotThrow(() -> bookService.updateBook(10L, update));
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenNullFields_whenUpdateBook_thenNoFieldsAreOverwritten() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null);

        BookDTO result = bookService.updateBook(10L, update);

        assertEquals("Clean Code", result.title());
        assertEquals("Prentice Hall", result.publisher());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenAllFieldsProvided_whenUpdateBook_thenAllFieldsAreUpdated() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO update = new BookDTO(null, "New Title", List.of("New Author"), "New Publisher",
                "New Description", 200, List.of("Fiction"), "new-thumbnail", "fr", 3.5, "9780000000000", 2020, false,
                List.of("Label"), "A", 1, 1);

        BookDTO result = bookService.updateBook(10L, update);

        assertEquals("New Title", result.title());
        assertEquals(List.of("New Author"), result.authors());
        assertEquals("New Publisher", result.publisher());
        assertEquals("New Description", result.description());
        assertEquals(200, result.pageCount());
        assertEquals(List.of("Fiction"), result.categories());
        assertEquals("new-thumbnail", result.thumbnail());
        assertEquals("fr", result.language());
        assertEquals("9780000000000", result.isbn());
        assertEquals(2020, result.publishedYear());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenZeroOrNegativePublishedYear_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, 0, false, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenBooksExist_whenGetAllBooksUnpaged_thenReturnsAllMappedDTOs() {
        when(bookRepository.findAll()).thenReturn(List.of(buildBook(), buildBook()));

        List<BookDTO> result = bookService.getAllBooksUnpaged();

        assertEquals(2, result.size());
        assertEquals("Clean Code", result.get(0).title());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenNoBooksExist_whenGetAllBooksUnpaged_thenReturnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<BookDTO> result = bookService.getAllBooksUnpaged();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenRepositoryFails_whenGetAllBooksUnpaged_thenThrowsException() {
        when(bookRepository.findAll()).thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class, () -> bookService.getAllBooksUnpaged());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenOneBookExists_whenGetAllBooksUnpaged_thenReturnsCorrectDTOMapping() {
        when(bookRepository.findAll()).thenReturn(List.of(buildBook()));

        List<BookDTO> result = bookService.getAllBooksUnpaged();

        assertEquals(1, result.size());
        BookDTO dto = result.get(0);
        assertEquals(10L, dto.id());
        assertEquals("Clean Code", dto.title());
        assertEquals(List.of("Robert C. Martin"), dto.authors());
        assertEquals("Prentice Hall", dto.publisher());
        assertEquals(464, dto.pageCount());
        assertEquals("en", dto.language());
        assertEquals(4.7, dto.rating());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenValidManualBookRequest_whenAddManualBook_thenSavesBookWithGeneratedNoIsbnUuid() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "  Manual Book  ",
                List.of(" Author One ", " ", "Author Two"),
                "  Publisher  ",
                "  Description  ",
                250,
                List.of(" Fantasy ", "", "Young adult"),
                "  thumbnail-url  ",
                "  nl  ",
                4.5,
                2024,
                true);

        BookEntity savedEntity = new BookEntity();
        savedEntity.setId(42L);
        savedEntity.setTitle("Manual Book");
        savedEntity.setAuthors(List.of("Author One", "Author Two"));
        savedEntity.setPublisher("Publisher");
        savedEntity.setDescription("Description");
        savedEntity.setPageCount(250);
        savedEntity.setCategories(List.of("Fantasy", "Young adult"));
        savedEntity.setThumbnail("thumbnail-url");
        savedEntity.setLanguage("nl");
        savedEntity.setRating(4.5);
        savedEntity.setPublishedYear(2024);
        savedEntity.setSpotlight(true);

        when(bookRepository.saveAndFlush(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity entity = invocation.getArgument(0);
            savedEntity.setIsbn(entity.getIsbn());
            return savedEntity;
        });

        when(bookRepository.findById(42L)).thenReturn(Optional.of(savedEntity));

        BookDTO result = bookService.addManualBook(request);

        assertNotNull(result);
        assertEquals(42L, result.id());
        assertEquals("Manual Book", result.title());
        assertEquals(List.of("Author One", "Author Two"), result.authors());
        assertEquals("Publisher", result.publisher());
        assertEquals("Description", result.description());
        assertEquals(250, result.pageCount());
        assertEquals(List.of("Fantasy", "Young adult"), result.categories());
        assertEquals("thumbnail-url", result.thumbnail());
        assertEquals("nl", result.language());
        assertEquals(4.5, result.rating());
        assertEquals(2024, result.publishedYear());
        assertNotNull(result.isbn());
        assertTrue(result.isbn().matches("^NOISBN-[0-9a-fA-F\\-]{36}$"));

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository, times(1)).saveAndFlush(captor.capture());
        verify(bookRepository, times(1)).findById(42L);

        BookEntity entityToSave = captor.getValue();
        assertEquals("Manual Book", entityToSave.getTitle());
        assertEquals(List.of("Author One", "Author Two"), entityToSave.getAuthors());
        assertEquals("Publisher", entityToSave.getPublisher());
        assertEquals("Description", entityToSave.getDescription());
        assertEquals(250, entityToSave.getPageCount());
        assertEquals(List.of("Fantasy", "Young adult"), entityToSave.getCategories());
        assertEquals("thumbnail-url", entityToSave.getThumbnail());
        assertEquals("nl", entityToSave.getLanguage());
        assertEquals(4.5, entityToSave.getRating());
        assertEquals(2024, entityToSave.getPublishedYear());
        assertTrue(entityToSave.isSpotlight());
        assertNotNull(entityToSave.getIsbn());
        assertTrue(entityToSave.getIsbn().matches("^NOISBN-[0-9a-fA-F\\-]{36}$"));
    }

    @Test
    void givenBlankTitle_whenAddManualBook_thenThrowsIllegalArgumentException() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "   ",
                List.of("Author"),
                "Publisher",
                "Description",
                100,
                List.of("Fantasy"),
                "thumbnail-url",
                "nl",
                4.0,
                2024,
                false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.addManualBook(request));

        assertEquals("Titel is verplicht", ex.getMessage());
        verify(bookRepository, never()).saveAndFlush(any(BookEntity.class));
        verify(bookRepository, never()).findById(anyLong());
    }

    @Test
    void givenNullOptionalFields_whenAddManualBook_thenUsesSafeDefaults() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        BookEntity savedEntity = new BookEntity();
        savedEntity.setId(7L);
        savedEntity.setTitle("Manual Book");
        savedEntity.setAuthors(List.of());
        savedEntity.setPublisher(null);
        savedEntity.setDescription(null);
        savedEntity.setPageCount(0);
        savedEntity.setCategories(List.of());
        savedEntity.setThumbnail(null);
        savedEntity.setLanguage(null);
        savedEntity.setRating(0.0);
        savedEntity.setPublishedYear(null);
        savedEntity.setSpotlight(false);

        when(bookRepository.saveAndFlush(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity entity = invocation.getArgument(0);
            savedEntity.setIsbn(entity.getIsbn());
            return savedEntity;
        });

        when(bookRepository.findById(7L)).thenReturn(Optional.of(savedEntity));

        BookDTO result = bookService.addManualBook(request);

        assertEquals(7L, result.id());
        assertEquals("Manual Book", result.title());
        assertEquals(List.of(), result.authors());
        assertEquals(0, result.pageCount());
        assertEquals(List.of(), result.categories());
        assertEquals(0.0, result.rating());
        assertNotNull(result.isbn());
        assertTrue(result.isbn().matches("^NOISBN-[0-9a-fA-F\\-]{36}$"));

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository).saveAndFlush(captor.capture());

        BookEntity entityToSave = captor.getValue();
        assertEquals("Manual Book", entityToSave.getTitle());
        assertEquals(List.of(), entityToSave.getAuthors());
        assertNull(entityToSave.getPublisher());
        assertNull(entityToSave.getDescription());
        assertEquals(0, entityToSave.getPageCount());
        assertEquals(List.of(), entityToSave.getCategories());
        assertNull(entityToSave.getThumbnail());
        assertNull(entityToSave.getLanguage());
        assertEquals(0.0, entityToSave.getRating());
        assertNull(entityToSave.getPublishedYear());
        assertFalse(entityToSave.isSpotlight());
        assertTrue(entityToSave.getIsbn().matches("^NOISBN-[0-9a-fA-F\\-]{36}$"));
    }

    // Helperfunctions

    private MockMultipartFile createExcelFile(String[][] rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Books");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ISBN");
            header.createCell(1).setCellValue("Title");

            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(rows[i][0]);
                row.createCell(1).setCellValue(rows[i][1]);
            }

            workbook.write(out);

            return new MockMultipartFile(
                    "file",
                    "books.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private Page<BookEntity> toEntityPage(List<BookEntity> list) {
        return new PageImpl<>(list, PageRequest.of(0, 20), list.size());
    }
}