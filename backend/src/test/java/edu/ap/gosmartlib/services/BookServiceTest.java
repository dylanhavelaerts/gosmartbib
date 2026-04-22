package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.BookDTO;
import edu.ap.gosmartlib.dto.CreateBookRequestDTO;
import edu.ap.gosmartlib.dto.BookInventoryDTO;
import edu.ap.gosmartlib.dto.CreateBookInventoryRequestDTO;
import edu.ap.gosmartlib.entities.BookInventoryEntity;
import edu.ap.gosmartlib.dto.googlebooks.GoogleBookItem;
import edu.ap.gosmartlib.dto.googlebooks.GoogleBooksResponse;
import edu.ap.gosmartlib.dto.googlebooks.VolumeInfo;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private BookService bookService; // Dit is de service die we écht testen

    private SchoolEntity school;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        // Omdat we geen echte Spring Boot context opstarten, vullen we de @Value url
        // handmatig in
        ReflectionTestUtils.setField(bookService, "googleBooksApiUrl",
                "https://www.googleapis.com/books/v1/volumes");
        ReflectionTestUtils.setField(bookService, "googleBooksApiKey", "test-key");

        school = new SchoolEntity();
        school.setId(1L);
        school.setName("AP Hogeschool");
        school.setDomain("https://aphogeschool.smartschool.be");

        user = new UserEntity();
        user.setSmartschoolUid("uid-123");
        user.setSchool(school);
        user.setRole(UserRoles.STUDENT);
    }

    // --- Google API Tests ---
    @Test
    void searchBookByIsbn_ShouldReturnPreview_WhenBookIsFound() {
        String isbn = "9798988421504";
        GoogleBooksResponse mockResponse = createMockGoogleResponse("Test Boek", "Test Auteur");

        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(mockResponse);

        BookDTO result = bookService.searchBookByIsbn(isbn);

        assertNotNull(result);
        assertEquals("Test Boek", result.title());
        assertNull(result.id());
        assertEquals(0, result.totalCopies());
        assertEquals(0, result.availableCopies());
        assertTrue(result.inventories().isEmpty());

        verifyNoInteractions(userRepository);
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void addBookByIsbn_ShouldSaveToDatabase_WhenBookIsFound() {
        String isbn = "9798988421504";
        GoogleBooksResponse mockResponse = createMockGoogleResponse("Gekocht Boek", "Auteur X");

        when(userRepository.findBySmartschoolUid("uid-123")).thenReturn(Optional.of(user));
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(mockResponse);
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BookDTO result = bookService.addBookByIsbn(isbn, "uid-123", "Campus Zuid");

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Gekocht Boek", result.title());
        assertEquals(1, result.totalCopies());
        assertEquals(1, result.availableCopies());
        assertEquals(1, result.inventories().size());
        assertEquals("Campus Zuid", result.inventories().get(0).campus());

        verify(bookRepository, times(1)).save(any(BookEntity.class));
    }

    @Test
    void searchBookByIsbn_ShouldThrowException_WhenNoBookFound() {
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class)))
                .thenReturn(new GoogleBooksResponse());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookService.searchBookByIsbn("9798988421507");
        });

        assertEquals("Geen boek voor ISBN: 9798988421507", exception.getMessage());
        verify(bookRepository, never()).save(any());
    }

    // --- getAllBooks Tests ---

    @Test
    void givenOneBookExists_whenGetAllBooks_thenReturnsCorrectDTOMapping() {
        when(bookRepository.findAllFiltered(eq(false), any(Pageable.class)))
                .thenReturn(toEntityPage(List.of(buildBook())));

        Page<BookDTO> result = bookService.getAllBooks(0, 20, UserRoles.STUDENT);

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

        verify(bookRepository, times(1)).findAllFiltered(eq(false), any(Pageable.class));
    }

    @Test
    void givenNoBooksExist_whenGetAllBooks_thenReturnsEmptyPage() {
        when(bookRepository.findAllFiltered(eq(false), any(Pageable.class)))
                .thenReturn(toEntityPage(List.of()));

        Page<BookDTO> result = bookService.getAllBooks(0, 20, UserRoles.STUDENT);

        assertEquals(0, result.getTotalElements());
        verify(bookRepository, times(1)).findAllFiltered(eq(false), any(Pageable.class));
    }

    @Test
    void givenMultipleBooksExist_whenGetAllBooks_thenReturnsAllBooks() {
        when(bookRepository.findAllFiltered(eq(false), any(Pageable.class)))
                .thenReturn(toEntityPage(List.of(buildBook(), buildBook())));

        Page<BookDTO> result = bookService.getAllBooks(0, 20, UserRoles.STUDENT);

        assertEquals(2, result.getTotalElements());
        verify(bookRepository, times(1)).findAllFiltered(eq(false), any(Pageable.class));
    }

    @Test
    void givenTeacherRole_whenGetAllBooks_thenIncludesDidacticBooks() {
        when(bookRepository.findAllFiltered(eq(true), any(Pageable.class)))
                .thenReturn(toEntityPage(List.of(buildBook())));

        Page<BookDTO> result = bookService.getAllBooks(0, 20, UserRoles.TEACHER);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findAllFiltered(eq(true), any(Pageable.class));
    }

    @Test
    void givenRepositoryFails_whenGetAllBooks_thenThrowsException() {
        when(bookRepository.findAllFiltered(anyBoolean(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class, () -> bookService.getAllBooks(0, 20, UserRoles.STUDENT));
    }

    @Test
    void givenNegativePage_whenGetAllBooks_thenThrowsException() {
        assertThrows(RuntimeException.class, () -> bookService.getAllBooks(-1, 20, UserRoles.STUDENT));
        verify(bookRepository, never()).findAllFiltered(anyBoolean(), any(Pageable.class));
    }

    @Test
    void givenZeroSize_whenGetAllBooks_thenThrowsException() {
        assertThrows(RuntimeException.class, () -> bookService.getAllBooks(0, 0, UserRoles.STUDENT));
        verify(bookRepository, never()).findAllFiltered(anyBoolean(), any(Pageable.class));
    }

    // --- getBookById Tests ---

    @Test
    void givenBookExists_whenGetBookById_thenReturnsCorrectDTO() {
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(buildBook()));

        BookDTO result = bookService.getBookById(10L, UserRoles.TEACHER);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Clean Code", result.title());
        verify(bookRepository, times(1)).findDetailedById(10L);
    }

    @Test
    void givenBookDoesNotExist_whenGetBookById_thenThrowsBookNotFoundException() {
        when(bookRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(99L, UserRoles.STUDENT));
        verify(bookRepository, times(1)).findDetailedById(99L);
    }

    @Test
    void givenDidacticBookAndStudentRole_whenGetBookById_thenThrowsForbidden() {
        BookEntity didacticBook = buildBook();
        didacticBook.setDidacticTag(true);
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(didacticBook));

        assertThrows(ResponseStatusException.class, () -> bookService.getBookById(10L, UserRoles.STUDENT));
        verify(bookRepository, times(1)).findDetailedById(10L);
    }

    @Test
    void givenDidacticBookAndTeacherRole_whenGetBookById_thenReturnsDTO() {
        BookEntity didacticBook = buildBook();
        didacticBook.setDidacticTag(true);
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(didacticBook));

        BookDTO result = bookService.getBookById(10L, UserRoles.TEACHER);

        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(bookRepository, times(1)).findDetailedById(10L);
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

    // --- getTop4BooksInSpotlight Tests ---

    @Test
    void givenSpotlightBookExists_whenGetTop4BooksInSpotlight_thenReturnsMappedDTOs() {
        BookEntity book1 = buildBook();
        book1.setId(1L);
        book1.setTitle("Spotlight Book 1");
        book1.setSpotlight(true);
        book1.setDidacticTag(false);

        BookEntity book2 = buildBook();
        book2.setId(2L);
        book2.setTitle("Spotlight Book 2");
        book2.setSpotlight(true);
        book2.setDidacticTag(false);

        BookEntity book4 = buildBook();
        book4.setId(4L);
        book4.setTitle("Spotlight Book 3");
        book4.setSpotlight(true);
        book4.setDidacticTag(false);

        BookEntity book5 = buildBook();
        book5.setId(5L);
        book5.setTitle("Spotlight Book 4");
        book5.setSpotlight(true);
        book5.setDidacticTag(false);

        when(bookRepository.findTop4BySpotlightTrueOrderByIdDesc()).thenReturn(List.of(book1, book2, book4, book5));

        List<BookDTO> result = bookService.getTop4BooksInSpotlight(UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("Spotlight Book 1", result.get(0).title());
        assertEquals("Spotlight Book 2", result.get(1).title());
        assertEquals("Spotlight Book 3", result.get(2).title());
        assertEquals("Spotlight Book 4", result.get(3).title());
        verify(bookRepository, times(1)).findTop4BySpotlightTrueOrderByIdDesc();
    }

    @Test
    void givenDidacticSpotlightBookAndStudentRole_whenGetTop4BooksInSpotlight_thenFiltersItOut() {
        BookEntity regular = buildBook();
        regular.setId(1L);
        regular.setTitle("Regular Spotlight");
        regular.setDidacticTag(false);

        BookEntity didactic = buildBook();
        didactic.setId(2L);
        didactic.setTitle("Didactic Spotlight");
        didactic.setDidacticTag(true);

        when(bookRepository.findTop4BySpotlightTrueOrderByIdDesc()).thenReturn(List.of(regular, didactic));

        List<BookDTO> result = bookService.getTop4BooksInSpotlight(UserRoles.STUDENT);

        assertEquals(1, result.size());
        assertEquals("Regular Spotlight", result.get(0).title());
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetTop4BooksInSpotlight_thenReturnsEmptyList() {
        when(bookRepository.findTop4BySpotlightTrueOrderByIdDesc()).thenReturn(List.of());

        List<BookDTO> result = bookService.getTop4BooksInSpotlight(UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findTop4BySpotlightTrueOrderByIdDesc();
    }

    // --- getLatestBooks Tests ---

    @Test
    void givenLatestBooksExist_whenGetLatestBooks_thenReturnsMappedDTOs() {
        BookEntity book2 = buildBook();
        book2.setId(10L);
        book2.setTitle("Newest Book 1");
        book2.setDidacticTag(false);

        BookEntity book3 = buildBook();
        book3.setId(11L);
        book3.setTitle("Newest Book 2");
        book3.setDidacticTag(false);

        BookEntity book4 = buildBook();
        book4.setId(12L);
        book4.setTitle("Newest Book 3");
        book4.setDidacticTag(false);

        BookEntity book5 = buildBook();
        book5.setId(13L);
        book5.setTitle("Newest Book 4");
        book5.setDidacticTag(false);

        when(bookRepository.findTop4ByOrderByIdDesc()).thenReturn(List.of(book2, book3, book4, book5));

        List<BookDTO> result = bookService.getLatestBooks(UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("Newest Book 1", result.get(0).title());
        verify(bookRepository, times(1)).findTop4ByOrderByIdDesc();
    }

    @Test
    void givenNoLatestBooksExist_whenGetLatestBooks_thenReturnsEmptyList() {
        when(bookRepository.findTop4ByOrderByIdDesc()).thenReturn(List.of());

        List<BookDTO> result = bookService.getLatestBooks(UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findTop4ByOrderByIdDesc();
    }

    // --- updateSpotlight Tests ---

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

    // --- getAllBooksInSpotlight Tests ---

    @Test
    void givenSpotlightBooksExist_whenGetAllBooksInSpotlight_thenReturnsMappedDTOs() {
        BookEntity book1 = buildBook();
        book1.setId(1L);
        book1.setTitle("Spotlight Book 1");
        book1.setSpotlight(true);
        book1.setDidacticTag(false);

        BookEntity book2 = buildBook();
        book2.setId(2L);
        book2.setTitle("Spotlight Book 2");
        book2.setSpotlight(true);
        book2.setDidacticTag(false);

        when(bookRepository.findBySpotlightTrueOrderByIdDesc()).thenReturn(List.of(book2, book1));

        List<BookDTO> result = bookService.getAllBooksInSpotlight(UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bookRepository, times(1)).findBySpotlightTrueOrderByIdDesc();
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetAllBooksInSpotlight_thenReturnsEmptyList() {
        when(bookRepository.findBySpotlightTrueOrderByIdDesc()).thenReturn(List.of());

        List<BookDTO> result = bookService.getAllBooksInSpotlight(UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findBySpotlightTrueOrderByIdDesc();
    }

    // --- searchByTitleOrAuthorOrCategory Tests ---

    @Test
    void givenNullQuery_whenSearchByTitleOrAuthorOrCategory_thenReturnsAllBooks() {
        when(bookRepository.findAllFiltered(eq(false), any(Pageable.class)))
                .thenReturn(toEntityPage(List.of(buildBook())));

        Page<BookDTO> result = bookService.searchByTitleOrAuthorOrCategory(null, 0, 20, UserRoles.STUDENT);

        assertEquals(1, result.getTotalElements());
        assertEquals("Clean Code", result.getContent().get(0).title());
        verify(bookRepository, times(1)).findAllFiltered(eq(false), any(Pageable.class));
        verify(bookRepository, never()).searchByTitleOrAuthorOrCategory(anyString(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void givenBlankQuery_whenSearchByTitleOrAuthorOrCategory_thenReturnsAllBooks() {
        when(bookRepository.findAllFiltered(eq(false), any(Pageable.class)))
                .thenReturn(toEntityPage(List.of(buildBook())));

        Page<BookDTO> result = bookService.searchByTitleOrAuthorOrCategory("   ", 0, 20, UserRoles.STUDENT);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findAllFiltered(eq(false), any(Pageable.class));
        verify(bookRepository, never()).searchByTitleOrAuthorOrCategory(anyString(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void givenValidQuery_whenSearchByTitleOrAuthorOrCategory_thenReturnsMatchingBooks() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.searchByTitleOrAuthorOrCategory(eq("Clean"), eq(false), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.searchByTitleOrAuthorOrCategory("Clean", 0, 20, UserRoles.STUDENT);

        assertEquals(1, result.getTotalElements());
        assertEquals("Clean Code", result.getContent().get(0).title());
        verify(bookRepository, times(1)).searchByTitleOrAuthorOrCategory(eq("Clean"), eq(false), any(Pageable.class));
        verify(bookRepository, never()).findAllFiltered(anyBoolean(), any(Pageable.class));
    }

    @Test
    void givenQueryWithWhitespace_whenSearchByTitleOrAuthorOrCategory_thenTrimsAndSearches() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.searchByTitleOrAuthorOrCategory(eq("Clean"), eq(false), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.searchByTitleOrAuthorOrCategory("  Clean  ", 0, 20, UserRoles.STUDENT);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).searchByTitleOrAuthorOrCategory(eq("Clean"), eq(false), any(Pageable.class));
    }

    @Test
    void givenNoMatchingBooks_whenSearchByTitleOrAuthorOrCategory_thenReturnsEmptyPage() {
        Page<BookEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(bookRepository.searchByTitleOrAuthorOrCategory(eq("Nonexistent"), eq(false), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<BookDTO> result = bookService.searchByTitleOrAuthorOrCategory("Nonexistent", 0, 20, UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(bookRepository, times(1))
                .searchByTitleOrAuthorOrCategory(eq("Nonexistent"), eq(false), any(Pageable.class));
    }

    @Test
    void givenTeacherRoleAndValidQuery_whenSearchByTitleOrAuthorOrCategory_thenPassesTrueToRepo() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.searchByTitleOrAuthorOrCategory(eq("Clean"), eq(true), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.searchByTitleOrAuthorOrCategory("Clean", 0, 20, UserRoles.TEACHER);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).searchByTitleOrAuthorOrCategory(eq("Clean"), eq(true), any(Pageable.class));
    }

    // --- filterBooks Service Tests ---

    @Test
    void givenValidFilters_whenFilterBooks_thenReturnsMappedDTOs() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.filterBooks(eq(false), eq("en"), eq(List.of("Programming")),
                eq(List.of("Toekomst & technologie")), eq(100), eq(500), eq(2000), eq(2023), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.filterBooks("en", List.of("Programming"),
                List.of("Toekomst & technologie"), 100, 500, 2000, 2023, 0, 20, UserRoles.STUDENT);

        assertEquals(1, result.getTotalElements());
        assertEquals("Clean Code", result.getContent().get(0).title());
        verify(bookRepository, times(1)).filterBooks(eq(false), eq("en"), eq(List.of("Programming")),
                eq(List.of("Toekomst & technologie")), eq(100), eq(500), eq(2000), eq(2023), any(Pageable.class));
    }

    @Test
    void givenNullFilters_whenFilterBooks_thenReturnsAllBooks() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook(), buildBook()), PageRequest.of(0, 20), 2);
        when(bookRepository.filterBooks(eq(false), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.filterBooks(null, null, null, null, null, null, null, 0, 20,
                UserRoles.STUDENT);

        assertEquals(2, result.getTotalElements());
        verify(bookRepository, times(1)).filterBooks(eq(false), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void givenOnlyLabels_whenFilterBooks_thenReturnsMatchingBooks() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.filterBooks(eq(false), isNull(), isNull(), eq(List.of("Toekomst & technologie")),
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.filterBooks(null, null, List.of("Toekomst & technologie"),
                null, null, null, null, 0, 20, UserRoles.STUDENT);

        assertEquals(1, result.getTotalElements());
        assertEquals("Clean Code", result.getContent().get(0).title());
        verify(bookRepository, times(1)).filterBooks(eq(false), isNull(), isNull(),
                eq(List.of("Toekomst & technologie")), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void givenNoMatchingBooks_whenFilterBooks_thenReturnsEmptyPage() {
        Page<BookEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(bookRepository.filterBooks(eq(false), eq("nl"), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<BookDTO> result = bookService.filterBooks("nl", null, null, null, null, null, null, 0, 20,
                UserRoles.STUDENT);

        assertEquals(0, result.getTotalElements());
        verify(bookRepository, times(1)).filterBooks(eq(false), eq("nl"), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void givenTeacherRole_whenFilterBooks_thenPassesTrueToRepo() {
        Page<BookEntity> entityPage = new PageImpl<>(List.of(buildBook()), PageRequest.of(0, 20), 1);
        when(bookRepository.filterBooks(eq(true), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<BookDTO> result = bookService.filterBooks(null, null, null, null, null, null, null, 0, 20,
                UserRoles.TEACHER);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).filterBooks(eq(true), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void givenMinPageCountGreaterThanMaxPageCount_whenFilterBooks_thenThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookService.filterBooks(null, null, null, 500, 100, null, null, 0, 20, UserRoles.STUDENT));

        verify(bookRepository, never()).filterBooks(anyBoolean(), any(), any(), any(), any(), any(), any(), any(),
                any(Pageable.class));
    }

    @Test
    void givenMinYearGreaterThanMaxYear_whenFilterBooks_thenThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookService.filterBooks(null, null, null, null, null, 2023, 2000, 0, 20, UserRoles.STUDENT));

        verify(bookRepository, never()).filterBooks(anyBoolean(), any(), any(), any(), any(), any(), any(), any(),
                any(Pageable.class));
    }

    @Test
    void givenRepositoryFails_whenFilterBooks_thenThrowsException() {
        when(bookRepository.filterBooks(anyBoolean(), any(), any(), any(), any(), any(), any(), any(),
                any(Pageable.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class,
                () -> bookService.filterBooks(null, null, null, null, null, null, null, 0, 20, UserRoles.STUDENT));
    }

    // --- getAllBooksUnpaged Tests ---

    @Test
    void givenBooksExist_whenGetAllBooksUnpaged_thenReturnsAllMappedDTOs() {
        BookEntity nonDidactic = buildBook();
        nonDidactic.setDidacticTag(false);
        when(bookRepository.findAll()).thenReturn(List.of(nonDidactic, nonDidactic));

        List<BookDTO> result = bookService.getAllBooksUnpaged(UserRoles.STUDENT);

        assertEquals(2, result.size());
        assertEquals("Clean Code", result.get(0).title());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenDidacticBookAndStudentRole_whenGetAllBooksUnpaged_thenFiltersItOut() {
        BookEntity regular = buildBook();
        regular.setDidacticTag(false);
        regular.setTitle("Regular Book");

        BookEntity didactic = buildBook();
        didactic.setDidacticTag(true);
        didactic.setTitle("Didactic Book");

        when(bookRepository.findAll()).thenReturn(List.of(regular, didactic));

        List<BookDTO> result = bookService.getAllBooksUnpaged(UserRoles.STUDENT);

        assertEquals(1, result.size());
        assertEquals("Regular Book", result.get(0).title());
    }

    @Test
    void givenDidacticBookAndTeacherRole_whenGetAllBooksUnpaged_thenIncludesIt() {
        BookEntity regular = buildBook();
        regular.setDidacticTag(false);

        BookEntity didactic = buildBook();
        didactic.setDidacticTag(true);

        when(bookRepository.findAll()).thenReturn(List.of(regular, didactic));

        List<BookDTO> result = bookService.getAllBooksUnpaged(UserRoles.TEACHER);

        assertEquals(2, result.size());
    }

    @Test
    void givenNoBooksExist_whenGetAllBooksUnpaged_thenReturnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<BookDTO> result = bookService.getAllBooksUnpaged(UserRoles.STUDENT);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenRepositoryFails_whenGetAllBooksUnpaged_thenThrowsException() {
        when(bookRepository.findAll()).thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class, () -> bookService.getAllBooksUnpaged(UserRoles.STUDENT));
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void givenOneBookExists_whenGetAllBooksUnpaged_thenReturnsCorrectDTOMapping() {
        BookEntity book = buildBook();
        book.setDidacticTag(false);
        when(bookRepository.findAll()).thenReturn(List.of(book));

        List<BookDTO> result = bookService.getAllBooksUnpaged(UserRoles.STUDENT);

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

    // --- importBooksFromExcel Tests ---

    @Test
    void givenEmptyExcelFile_whenImportBooksFromExcel_thenThrowsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.importBooksFromExcel(file, "uid-123", "Campus Zuid"));

        assertEquals("Upload een excel file die niet leeg is", ex.getMessage());
        verifyNoInteractions(bookRepository, restTemplate);
    }

    @Test
    void givenValidExcelRow_whenImportBooksFromExcel_thenSavesBookAndReturnsCorrectSummary() throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "Clean Code" }
        });

        GoogleBooksResponse googleResponse = createMockGoogleResponse("Clean Code", "Robert C. Martin");

        when(userRepository.findBySmartschoolUid("uid-123")).thenReturn(Optional.of(user));
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class))).thenReturn(googleResponse);
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file, "uid-123", "Campus Zuid");

        assertEquals(1, result.totalRows());
        assertEquals(1, result.savedCount());
        assertEquals(0, result.mismatchCount());
        assertTrue(result.mismatches().isEmpty());

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository, times(1)).save(captor.capture());
        assertEquals("Clean Code", captor.getValue().getTitle());
        assertEquals("9780132350884", captor.getValue().getIsbn());
    }

    @Test
    void givenBookAlreadyExistsInDatabase_whenImportBooksFromExcel_thenAddsMismatchAndDoesNotSave() throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "Clean Code" }
        });

        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file, "uid-123", "Campus Zuid");

        assertEquals(1, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(1, result.mismatchCount());
        verify(bookRepository, never()).save(any(BookEntity.class));
        verify(restTemplate, never()).getForObject(anyString(), eq(GoogleBooksResponse.class));
    }

    @Test
    void givenExcelTitleDoesNotMatchGoogleTitle_whenImportBooksFromExcel_thenAddsMismatchAndDoesNotSave()
            throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "Clean Code" }
        });

        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(userRepository.findBySmartschoolUid("uid-123")).thenReturn(Optional.of(user));
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class)))
                .thenReturn(createMockGoogleResponse("Refactoring", "Martin Fowler"));

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file, "uid-123", "Campus Zuid");

        assertEquals(1, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(1, result.mismatchCount());
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

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file, "uid-123", "Campus Zuid");

        assertEquals(1, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(1, result.mismatchCount());
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenRowWithMissingIsbnOrTitle_whenImportBooksFromExcel_thenAddsMismatchAndDoesNotCallGoogle()
            throws IOException {
        MockMultipartFile file = createExcelFile(new String[][] {
                { "9780132350884", "" },
                { "", "Clean Code" }
        });

        BulkImportResponseDTO result = bookService.importBooksFromExcel(file, "uid-123", "Campus Zuid");

        assertEquals(2, result.totalRows());
        assertEquals(0, result.savedCount());
        assertEquals(2, result.mismatchCount());
        verify(bookRepository, never()).save(any(BookEntity.class));
        verify(restTemplate, never()).getForObject(anyString(), eq(GoogleBooksResponse.class));
    }

    @Test
    void givenUnreadableExcelFile_whenImportBooksFromExcel_thenThrowsRuntimeException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookService.importBooksFromExcel(file, "uid-123", "Campus Zuid"));
        assertEquals("Kon de excel file niet lezen", ex.getMessage());
    }

    // --- updateBook Service Tests ---

    @Test
    void givenBookExists_whenUpdateBook_thenUpdatesFieldsAndReturnsMappedDTO() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO update = new BookDTO(null, "Refactoring", List.of("Martin Fowler"), null, null,
                null, null, null, null, null, null, null, false, null, null, null, null, null, null);

        BookDTO result = bookService.updateBook(10L, update);

        assertEquals("Refactoring", result.title());
        assertEquals(List.of("Martin Fowler"), result.authors());
        assertEquals("Prentice Hall", result.publisher());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenBookDoesNotExist_whenUpdateBook_thenThrowsBookNotFoundException() {
        when(bookRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        BookDTO update = new BookDTO(null, "New Title", null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null, null, null);

        assertThrows(BookNotFoundException.class, () -> bookService.updateBook(99L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenBlankTitle_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, "   ", null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenEmptyTitle_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, "", null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenNegativePageCount_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, null, null, null, null,
                -1, null, null, null, null, null, null, false, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenFuturePublishedYear_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));

        int futureYear = Year.now().getValue() + 1;
        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, futureYear, false, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenCurrentYear_whenUpdateBook_thenSavesSuccessfully() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int currentYear = Year.now().getValue();
        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, currentYear, false, null, null, null, null, null, null);

        assertDoesNotThrow(() -> bookService.updateBook(10L, update));
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenNullFields_whenUpdateBook_thenNoFieldsAreOverwritten() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null, null, null);

        BookDTO result = bookService.updateBook(10L, update);

        assertEquals("Clean Code", result.title());
        assertEquals("Prentice Hall", result.publisher());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void givenAllFieldsProvided_whenUpdateBook_thenAllFieldsAreUpdated() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO update = new BookDTO(null, "New Title", List.of("New Author"), "New Publisher",
                "New Description", 200, List.of("Fiction"), "new-thumbnail", "fr", 3.5, "9780000000000", 2020, false,
                List.of("Label"), "A", 1, 1, "Eerste graad", null);

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
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(null, null, null, null, null,
                null, null, null, null, null, null, 0, false, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(10L, update));
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    // --- addManualBook Tests ---

    @Test
    void givenValidManualBookRequest_whenAddManualBook_thenSavesBookWithGeneratedNoIsbnUuid() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "  Manual Book  ", List.of(" Author One ", " ", "Author Two"), "  Publisher  ", "  Description  ",
                250, List.of(" Fantasy ", "", "Young adult"), "  thumbnail-url  ", "  nl  ", 4.5, 2024,
                true, false, null, "A", 1, 1, "Eerste graad", null);

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
        savedEntity.setInventories(new ArrayList<>());

        when(userRepository.findBySmartschoolUid("uid-123")).thenReturn(Optional.of(user));
        when(bookRepository.saveAndFlush(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity entity = invocation.getArgument(0);
            savedEntity.setIsbn(entity.getIsbn());
            return savedEntity;
        });
        when(bookRepository.findDetailedById(42L)).thenReturn(Optional.of(savedEntity));

        BookDTO result = bookService.addManualBook(request, "uid-123");

        assertNotNull(result);
        assertEquals(42L, result.id());
        assertEquals("Manual Book", result.title());
        assertNotNull(result.isbn());
        assertTrue(result.isbn().matches("^NOISBN-[0-9a-fA-F\\-]{36}$"));

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository, times(1)).saveAndFlush(captor.capture());
        verify(bookRepository, times(1)).findDetailedById(42L);
        assertEquals("Manual Book", captor.getValue().getTitle());
        assertTrue(captor.getValue().getIsbn().matches("^NOISBN-[0-9a-fA-F\\-]{36}$"));
    }

    @Test
    void givenBlankTitle_whenAddManualBook_thenThrowsIllegalArgumentException() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "   ", List.of("Author"), "Publisher", "Description", 100,
                List.of("Fantasy"), "thumbnail-url", "nl", 4.0, 2024,
                false, false, null, "A", 1, 1, "Eerste graad", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookService.addManualBook(request, "uid-123"));

        assertEquals("Titel is verplicht", ex.getMessage());
        verify(bookRepository, never()).saveAndFlush(any(BookEntity.class));
        verify(bookRepository, never()).findDetailedById(anyLong());
    }

    @Test
    void givenNullOptionalFields_whenAddManualBook_thenUsesSafeDefaults() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book", null, null, null, null, null, null, null, null, null, null, false, null, null, null,
                null, null, null);

        BookEntity savedEntity = new BookEntity();
        savedEntity.setId(7L);
        savedEntity.setTitle("Manual Book");
        savedEntity.setAuthors(List.of());
        savedEntity.setPageCount(0);
        savedEntity.setCategories(List.of());
        savedEntity.setRating(0.0);
        savedEntity.setSpotlight(false);
        savedEntity.setInventories(new ArrayList<>());

        when(userRepository.findBySmartschoolUid("uid-123")).thenReturn(Optional.of(user));
        when(bookRepository.saveAndFlush(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity entity = invocation.getArgument(0);
            savedEntity.setIsbn(entity.getIsbn());
            return savedEntity;
        });
        when(bookRepository.findDetailedById(7L)).thenReturn(Optional.of(savedEntity));

        BookDTO result = bookService.addManualBook(request, "uid-123");

        assertEquals(7L, result.id());
        assertEquals("Manual Book", result.title());
        assertEquals(List.of(), result.authors());
        assertEquals(0, result.pageCount());
        assertEquals(0.0, result.rating());
        assertNotNull(result.isbn());
        assertTrue(result.isbn().matches("^NOISBN-[0-9a-fA-F\\-]{36}$"));
    }

    @Test
    void givenExplicitSchoolInventories_whenAddManualBook_thenUsesThoseInventoriesAndRecomputesTotals() {
        SchoolEntity school2 = new SchoolEntity();
        school2.setId(2L);
        school2.setName("GO! School");
        school2.setDomain("https://go.smartschool.be");

        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book",
                List.of("Author One"),
                "Publisher",
                "Description",
                200,
                List.of("Programming"),
                "thumbnail-url",
                "nl",
                4.5,
                2024,
                false,
                false,
                List.of("Toekomst & technologie"),
                "A",
                null,
                null,
                "Eerste graad",
                List.of(
                        new CreateBookInventoryRequestDTO(1L, "Campus Noord", 2, 1),
                        new CreateBookInventoryRequestDTO(2L, "Campus Zuid", 3, 2)));

        BookEntity reloaded = new BookEntity();
        reloaded.setId(42L);
        reloaded.setTitle("Manual Book");
        reloaded.setTotalCopies(5);
        reloaded.setAvailableCopies(3);
        reloaded.setInventories(new ArrayList<>(List.of(
                buildInventory(school, "Campus Noord", 2, 1),
                buildInventory(school2, "Campus Zuid", 3, 2))));

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(school2));
        when(bookRepository.saveAndFlush(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return entity;
        });
        when(bookRepository.findDetailedById(42L)).thenReturn(Optional.of(reloaded));

        BookDTO result = bookService.addManualBook(request, "uid-123");

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository).saveAndFlush(captor.capture());

        BookEntity persisted = captor.getValue();
        assertEquals(2, persisted.getInventories().size());
        assertEquals(5, persisted.getTotalCopies());
        assertEquals(3, persisted.getAvailableCopies());

        assertEquals(1L, persisted.getInventories().get(0).getSchool().getId());
        assertEquals("Campus Noord", persisted.getInventories().get(0).getCampus());
        assertEquals(2, persisted.getInventories().get(0).getTotalCopies());
        assertEquals(1, persisted.getInventories().get(0).getAvailableCopies());

        assertEquals(2L, persisted.getInventories().get(1).getSchool().getId());
        assertEquals("Campus Zuid", persisted.getInventories().get(1).getCampus());
        assertEquals(3, persisted.getInventories().get(1).getTotalCopies());
        assertEquals(2, persisted.getInventories().get(1).getAvailableCopies());

        assertEquals(42L, result.id());
        assertEquals(5, result.totalCopies());
        assertEquals(3, result.availableCopies());
        assertEquals(2, result.inventories().size());
    }

    @Test
    void givenNoExplicitInventories_whenAddManualBook_thenCreatesInventoryForCurrentUsersSchool() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book",
                List.of("Author One"),
                "Publisher",
                "Description",
                200,
                List.of("Programming"),
                "thumbnail-url",
                "nl",
                4.5,
                2024,
                false,
                false,
                List.of("Toekomst & technologie"),
                "A",
                4,
                2,
                "Eerste graad",
                null);

        BookEntity reloaded = new BookEntity();
        reloaded.setId(7L);
        reloaded.setTitle("Manual Book");
        reloaded.setTotalCopies(4);
        reloaded.setAvailableCopies(2);
        reloaded.setInventories(new ArrayList<>(List.of(
                buildInventory(school, null, 4, 2))));

        when(userRepository.findBySmartschoolUid("uid-123")).thenReturn(Optional.of(user));
        when(bookRepository.saveAndFlush(any(BookEntity.class))).thenAnswer(invocation -> {
            BookEntity entity = invocation.getArgument(0);
            entity.setId(7L);
            return entity;
        });
        when(bookRepository.findDetailedById(7L)).thenReturn(Optional.of(reloaded));

        BookDTO result = bookService.addManualBook(request, "uid-123");

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookRepository).saveAndFlush(captor.capture());

        BookEntity persisted = captor.getValue();
        assertEquals(1, persisted.getInventories().size());
        assertEquals(1L, persisted.getInventories().get(0).getSchool().getId());
        assertEquals("", persisted.getInventories().get(0).getCampus());
        assertEquals(4, persisted.getInventories().get(0).getTotalCopies());
        assertEquals(2, persisted.getInventories().get(0).getAvailableCopies());

        assertEquals(4, persisted.getTotalCopies());
        assertEquals(2, persisted.getAvailableCopies());

        assertEquals(7L, result.id());
        assertEquals(4, result.totalCopies());
        assertEquals(2, result.availableCopies());
        assertEquals(1, result.inventories().size());
    }

    @Test
    void givenInventoryList_whenUpdateBook_thenReplacesInventoriesAndRecomputesTotals() {
        SchoolEntity school2 = new SchoolEntity();
        school2.setId(2L);
        school2.setName("GO! School");
        school2.setDomain("https://go.smartschool.be");

        BookEntity book = buildBook();
        book.setInventories(new ArrayList<>(List.of(
                buildInventory(school, "Old Campus", 1, 1))));
        book.setTotalCopies(1);
        book.setAvailableCopies(1);

        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(school2));
        when(bookRepository.save(any(BookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO update = new BookDTO(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new BookInventoryDTO(null, 1L, "AP Hogeschool", "Campus A", 4, 3),
                        new BookInventoryDTO(null, 2L, "GO! School", "Campus B", 6, 4)));

        BookDTO result = bookService.updateBook(10L, update);

        assertEquals(2, book.getInventories().size());
        assertEquals(10, book.getTotalCopies());
        assertEquals(7, book.getAvailableCopies());

        assertEquals(1L, book.getInventories().get(0).getSchool().getId());
        assertEquals("Campus A", book.getInventories().get(0).getCampus());
        assertEquals(4, book.getInventories().get(0).getTotalCopies());
        assertEquals(3, book.getInventories().get(0).getAvailableCopies());

        assertEquals(2L, book.getInventories().get(1).getSchool().getId());
        assertEquals("Campus B", book.getInventories().get(1).getCampus());
        assertEquals(6, book.getInventories().get(1).getTotalCopies());
        assertEquals(4, book.getInventories().get(1).getAvailableCopies());

        assertEquals(10, result.totalCopies());
        assertEquals(7, result.availableCopies());
        assertEquals(2, result.inventories().size());

        verify(bookRepository).save(book);
    }

    @Test
    void givenInventoryWithAvailableCopiesGreaterThanTotal_whenUpdateBook_thenThrowsIllegalArgumentException() {
        BookEntity book = buildBook();
        when(bookRepository.findDetailedById(10L)).thenReturn(Optional.of(book));

        BookDTO update = new BookDTO(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new BookInventoryDTO(null, 1L, "AP Hogeschool", "Campus A", 2, 3)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.updateBook(10L, update));

        assertEquals("Beschikbare exemplaren mogen niet groter zijn dan totaal aantal exemplaren", ex.getMessage());
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    void givenUnknownSchoolInInventory_whenAddManualBook_thenThrowsIllegalArgumentException() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book",
                List.of("Author One"),
                "Publisher",
                "Description",
                200,
                List.of("Programming"),
                "thumbnail-url",
                "nl",
                4.5,
                2024,
                false,
                false,
                List.of("Toekomst & technologie"),
                "A",
                null,
                null,
                "Eerste graad",
                List.of(new CreateBookInventoryRequestDTO(999L, "Campus X", 2, 1)));

        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookService.addManualBook(request, "uid-123"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("School niet gevonden voor id: 999", ex.getReason());
    }

    // --- Helpers ---

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
                2008, true, List.of("Toekomst & technologie"), "A", 1, 1, "Eerste graad");
        book.setId(10L);
        book.setSpotlight(true);
        book.setTotalCopies(5);
        book.setAvailableCopies(5);
        book.setInventories(new ArrayList<>());
        return book;
    }

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
                    "file", "books.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private Page<BookEntity> toEntityPage(List<BookEntity> list) {
        return new PageImpl<>(list, PageRequest.of(0, 20), list.size());
    }

    private BookInventoryEntity buildInventory(SchoolEntity school, String campus, int totalCopies,
            int availableCopies) {
        BookInventoryEntity inventory = new BookInventoryEntity();
        inventory.setSchool(school);
        inventory.setCampus(campus);
        inventory.setTotalCopies(totalCopies);
        inventory.setAvailableCopies(availableCopies);
        return inventory;
    }
}