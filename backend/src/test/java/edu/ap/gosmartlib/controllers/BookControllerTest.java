package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.BookDTO;
import edu.ap.gosmartlib.dto.BookFilterRequest;
import edu.ap.gosmartlib.dto.CreateBookRequestDTO;
import edu.ap.gosmartlib.dto.BookInventoryDTO;
import edu.ap.gosmartlib.dto.CreateBookInventoryRequestDTO;
import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.dto.importdto.ImportMismatchDTO;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.BookService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.isNull;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;
    @Mock
    private UserRepository userRepository;

    // @Spy gebruikt de echte implementatie van AuthHelper zodat extractUid/extractUidOrNull
    // correct werken zonder elke test afzonderlijk te stubben.
    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private BookController bookController;

    private BookDTO buildDTO(Long id, String title) {
        return new BookDTO(id, title, List.of("Author"), "Publisher", "Description",
                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890",
                2023, false, null, "A", 1, 1, "Eerste graad", "https://books.google.com/preview", null);
    }

    // authentication=null → callerRole() returns STUDENT (most restrictive
    // fallback)

    @Test
    void givenBooksExist_whenGetBooks_thenReturnsExpectedDTOs() {
        List<BookDTO> books = List.of(buildDTO(1L, "Clean Code"));
        Page<BookDTO> expected = toPage(books);
        when(bookService.getAllBooks(0, 20, UserRoles.STUDENT, null)).thenReturn(expected);

        Page<BookDTO> result = bookController.getBooks(0, 20, null);

        assertEquals(expected, result);
        verify(bookService, times(1)).getAllBooks(0, 20, UserRoles.STUDENT, null);
    }

    @Test
    void givenNoBooksExist_whenGetBooks_thenReturnsEmptyPage() {
        Page<BookDTO> expected = toPage(List.of());
        when(bookService.getAllBooks(0, 20, UserRoles.STUDENT, null)).thenReturn(expected);

        Page<BookDTO> result = bookController.getBooks(0, 20, null);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(bookService, times(1)).getAllBooks(0, 20, UserRoles.STUDENT, null);
    }

    @Test
    void givenMultipleBooksExist_whenGetBooks_thenReturnsAllBooks() {
        List<BookDTO> books = List.of(
                buildDTO(1L, "Clean Code"),
                buildDTO(2L, "Effective Java"),
                buildDTO(3L, "Refactoring"));
        Page<BookDTO> expected = toPage(books);
        when(bookService.getAllBooks(0, 20, UserRoles.STUDENT, null)).thenReturn(expected);

        Page<BookDTO> result = bookController.getBooks(0, 20, null);

        assertEquals(3, result.getContent().size());
        assertEquals("Clean Code", result.getContent().get(0).title());
        assertEquals("Effective Java", result.getContent().get(1).title());
        assertEquals("Refactoring", result.getContent().get(2).title());
        verify(bookService, times(1)).getAllBooks(0, 20, UserRoles.STUDENT, null);
    }

    @Test
    void givenServiceFails_whenGetBooks_thenThrowsException() {
        when(bookService.getAllBooks(0, 20, UserRoles.STUDENT, null))
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(RuntimeException.class, () -> bookController.getBooks(0, 20, null));
        verify(bookService, times(1)).getAllBooks(0, 20, UserRoles.STUDENT, null);
    }

    @Test
    void givenServiceIsCalled_whenGetBooks_thenDelegatesOnlyToService() {
        when(bookService.getAllBooks(0, 20, UserRoles.STUDENT, null)).thenReturn(toPage(List.of()));

        bookController.getBooks(0, 20, null);

        verify(bookService, times(1)).getAllBooks(0, 20, UserRoles.STUDENT, null);
        verifyNoMoreInteractions(bookService);
    }

    @Test
    void givenBookExists_whenGetBookById_thenReturnsCorrectDTO() throws BookNotFoundException {
        BookDTO expected = buildDTO(1L, "Clean Code");
        when(bookService.getBookById(1L, UserRoles.STUDENT, null)).thenReturn(expected);

        ResponseEntity<?> result = bookController.getBookById(1L, null);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).getBookById(1L, UserRoles.STUDENT, null);
    }

    @Test
    void givenBookDoesNotExist_whenGetBookById_thenReturnsNotFound() throws BookNotFoundException {
        when(bookService.getBookById(99L, UserRoles.STUDENT, null)).thenThrow(new BookNotFoundException(99L));

        ResponseEntity<?> result = bookController.getBookById(99L, null);

        assertEquals(404, result.getStatusCode().value());
        verify(bookService, times(1)).getBookById(99L, UserRoles.STUDENT, null);
    }



    @Test
    void givenSpotlightBooksExist_whenGetBooksInSpotlight_thenReturnsExpectedDTOs() {
        List<BookDTO> expected = List.of(
                buildDTO(1L, "Spotlight Book 1"),
                buildDTO(2L, "Spotlight Book 2"));
        when(bookService.getTop4BooksInSpotlight(UserRoles.STUDENT, null)).thenReturn(expected);

        List<BookDTO> result = bookController.getBooksInSpotlight(null);

        assertEquals(expected, result);
        verify(bookService, times(1)).getTop4BooksInSpotlight(UserRoles.STUDENT, null);
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetBooksInSpotlight_thenReturnsEmptyList() {
        when(bookService.getTop4BooksInSpotlight(UserRoles.STUDENT, null)).thenReturn(List.of());

        List<BookDTO> result = bookController.getBooksInSpotlight(null);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getTop4BooksInSpotlight(UserRoles.STUDENT, null);
    }

    @Test
    void givenLatestBooksExist_whenGetLatestBooks_thenReturnsExpectedDTOs() {
        List<BookDTO> expected = List.of(
                buildDTO(10L, "Latest Book 1"),
                buildDTO(11L, "Latest Book 2"),
                buildDTO(12L, "Latest Book 3"));
        when(bookService.getLatestBooks(UserRoles.STUDENT, null)).thenReturn(expected);

        List<BookDTO> result = bookController.getLatestBooks(null);

        assertEquals(expected, result);
        verify(bookService, times(1)).getLatestBooks(UserRoles.STUDENT, null);
    }

    @Test
    void givenNoLatestBooksExist_whenGetLatestBooks_thenReturnsEmptyList() {
        when(bookService.getLatestBooks(UserRoles.STUDENT, null)).thenReturn(List.of());

        List<BookDTO> result = bookController.getLatestBooks(null);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getLatestBooks(UserRoles.STUDENT, null);
    }

    // --- filterBooks Controller Tests ---

    @Test
    void givenValidFilters_whenFilterBooks_thenReturnsOk() {
        BookFilterRequest filter = new BookFilterRequest(
                null, "en", List.of("Programming"), List.of("Toekomst & technologie"),
                100, 500, 2000, 2023, 3.0, 5.0, null, 0, 20);
        Page<BookDTO> expected = toPage(List.of(buildDTO(1L, "Clean Code")));
        when(bookService.filterBooks(filter, UserRoles.STUDENT, null))
                .thenReturn(expected);

        ResponseEntity<?> result = bookController.filterBooks(filter, null);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).filterBooks(filter, UserRoles.STUDENT, null);
    }

    @Test
    void givenOnlyLabels_whenFilterBooks_thenReturnsOk() {
        BookFilterRequest filter = new BookFilterRequest(
                null, null, null, List.of("Toekomst & technologie"),
                null, null, null, null, null, null, null, 0, 20);
        Page<BookDTO> expected = toPage(List.of(buildDTO(1L, "Clean Code")));
        when(bookService.filterBooks(filter, UserRoles.STUDENT, null))
                .thenReturn(expected);

        ResponseEntity<?> result = bookController.filterBooks(filter, null);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).filterBooks(filter, UserRoles.STUDENT, null);
    }

    @Test
    void givenNullFilters_whenFilterBooks_thenReturnsOk() {
        BookFilterRequest filter = new BookFilterRequest(
                null, null, null, null,
                null, null, null, null, null, null, null, 0, 20);
        when(bookService.filterBooks(filter, UserRoles.STUDENT, null))
                .thenReturn(toPage(List.of(buildDTO(1L, "Clean Code"))));

        ResponseEntity<?> result = bookController.filterBooks(filter, null);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void givenMinGreaterThanMax_whenFilterBooks_thenReturnsBadRequest() {
        BookFilterRequest filter = new BookFilterRequest(
                null, null, null, null,
                500, 100, null, null, null, null, null, 0, 20);
        when(bookService.filterBooks(filter, UserRoles.STUDENT, null))
                .thenThrow(new IllegalArgumentException("minPageCount cannot be bigger than maxPageCount"));

        ResponseEntity<?> result = bookController.filterBooks(filter, null);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("minPageCount cannot be bigger than maxPageCount", result.getBody());
    }

    @Test
    void givenMinRatingGreaterThanMaxRating_whenFilterBooks_thenReturnsBadRequest() {
        BookFilterRequest filter = new BookFilterRequest(
                null, null, null, null,
                null, null, null, null, 5.0, 3.0, null, 0, 20);
        when(bookService.filterBooks(filter, UserRoles.STUDENT, null))
                .thenThrow(new IllegalArgumentException("minRating mag niet groter zijn dan maxRating"));

        ResponseEntity<?> result = bookController.filterBooks(filter, null);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("minRating mag niet groter zijn dan maxRating", result.getBody());
    }

    @Test
    void givenDatabaseFails_whenFilterBooks_thenReturnsInternalServerError() {
        when(bookService.filterBooks(any(BookFilterRequest.class), any(), isNull()))
                .thenThrow(new DataAccessException("DB down") {
                });

        ResponseEntity<?> result = bookController.filterBooks(new BookFilterRequest(
                null, null, null, null,
                null, null, null, null, null, null, null, 0, 20), null);

        assertEquals(500, result.getStatusCode().value());
    }

    @Test
    void givenNoResults_whenFilterBooks_thenReturnsEmptyPage() {
        when(bookService.filterBooks(any(BookFilterRequest.class), any(), isNull()))
                .thenReturn(toPage(List.of()));

        ResponseEntity<?> result = bookController.filterBooks(new BookFilterRequest(
                null, null, null, null,
                null, null, null, null, null, null, null, 0, 20), null);

        assertEquals(200, result.getStatusCode().value());
        Page<?> body = (Page<?>) result.getBody();
        assertNotNull(body);
        assertEquals(0, body.getTotalElements());
    }

    // --- spotlight Controller Tests ---

    @Test
    void givenSpotlightBooksExist_whenGetAllBooksInSpotlight_thenReturnsExpectedDTOs() {
        List<BookDTO> expected = List.of(
                buildDTO(1L, "Spotlight Book 1"),
                buildDTO(2L, "Spotlight Book 2"),
                buildDTO(3L, "Spotlight Book 3"));

        when(bookService.getAllBooksInSpotlight(UserRoles.STUDENT, null)).thenReturn(expected);

        List<BookDTO> result = bookController.getAllBooksInSpotlight(null);

        assertEquals(expected, result);
        verify(bookService, times(1)).getAllBooksInSpotlight(UserRoles.STUDENT, null);
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetAllBooksInSpotlight_thenReturnsEmptyList() {
        when(bookService.getAllBooksInSpotlight(UserRoles.STUDENT, null)).thenReturn(List.of());

        List<BookDTO> result = bookController.getAllBooksInSpotlight(null);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getAllBooksInSpotlight(UserRoles.STUDENT, null);
    }

    @Test
    void givenBookExists_whenUpdateSpotlight_thenReturnsNoContent() throws BookNotFoundException {
        ResponseEntity<Void> result = bookController.updateSpotlight(1L, true);

        assertEquals(204, result.getStatusCode().value());
        verify(bookService, times(1)).updateSpotlight(1L, true);
    }

    @Test
    void givenBookDoesNotExist_whenUpdateSpotlight_thenThrowsBookNotFoundException() throws BookNotFoundException {
        doThrow(new BookNotFoundException(99L)).when(bookService).updateSpotlight(99L, false);

        assertThrows(BookNotFoundException.class, () -> bookController.updateSpotlight(99L, false));
        verify(bookService, times(1)).updateSpotlight(99L, false);
    }

    @Test
    void givenQuery_whenSearchByTitleOrAuthor_thenReturnsOkWithResults() {
        Page<BookDTO> expected = toPage(List.of(buildDTO(1L, "Clean Code")));
        when(bookService.searchByTitleOrAuthorOrCategory("Clean", 0, 20, UserRoles.STUDENT, null)).thenReturn(expected);

        ResponseEntity<Page<BookDTO>> result = bookController.searchByTitleOrAuthorOrCategory("Clean", 0, 20, null);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).searchByTitleOrAuthorOrCategory("Clean", 0, 20, UserRoles.STUDENT, null);
    }

    @Test
    void givenValidExcelFile_whenImportBooks_thenReturnsOkWithImportSummary() {
        OAuth2User principal = mockPrincipal("uid-123");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        BulkImportResponseDTO expected = new BulkImportResponseDTO(
                3, 2, 1,
                List.of(new ImportMismatchDTO(4, "9780132350884", "Wrong Title", "Clean Code",
                        "De titel komt niet overeen (Clean Code)")));

        when(bookService.importBooksFromExcel(any(MultipartFile.class), eq("uid-123"), eq("Campus Zuid")))
                .thenReturn(expected);

        ResponseEntity<?> result = bookController.importBooks(file, "Campus Zuid", principal);

        assertEquals(200, result.getStatusCode().value());
        assertSame(expected, result.getBody());
        verify(bookService, times(1)).importBooksFromExcel(file, "uid-123", "Campus Zuid");
    }

    @Test
    void givenInvalidExcelFile_whenImportBooks_thenReturnsBadRequest() {
        OAuth2User principal = mockPrincipal("uid-123");

        MockMultipartFile file = new MockMultipartFile(
                "file", "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        when(bookService.importBooksFromExcel(any(MultipartFile.class), eq("uid-123"), eq("Campus Zuid")))
                .thenThrow(new IllegalArgumentException("Upload een excel file die niet leeg is"));

        ResponseEntity<?> result = bookController.importBooks(file, "Campus Zuid", principal);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Upload een excel file die niet leeg is", result.getBody());
        verify(bookService, times(1)).importBooksFromExcel(file, "uid-123", "Campus Zuid");
    }

    @Test
    void givenUnexpectedServiceError_whenImportBooks_thenReturnsInternalServerError() {
        OAuth2User principal = mockPrincipal("uid-123");

        MockMultipartFile file = new MockMultipartFile(
                "file", "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        when(bookService.importBooksFromExcel(any(MultipartFile.class), eq("uid-123"), eq("Campus Zuid")))
                .thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> result = bookController.importBooks(file, "Campus Zuid", principal);

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while importing the Excel file.", result.getBody());
        verify(bookService, times(1)).importBooksFromExcel(file, "uid-123", "Campus Zuid");
    }

    // --- updateBook Controller Tests ---

    @Test
    void givenBookExists_whenUpdateBook_thenReturnsOkWithUpdatedDTO() {
        BookDTO updatedDTO = buildDTO(1L, "Updated Title");
        when(bookService.updateBook(1L, updatedDTO)).thenReturn(updatedDTO);

        ResponseEntity<?> result = bookController.updateBook(1L, updatedDTO);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(updatedDTO, result.getBody());
        verify(bookService, times(1)).updateBook(1L, updatedDTO);
    }

    @Test
    void givenBookDoesNotExist_whenUpdateBook_thenReturnsNotFound() {
        BookDTO updatedDTO = buildDTO(99L, "Some Title");
        when(bookService.updateBook(99L, updatedDTO)).thenThrow(new BookNotFoundException(99L));

        ResponseEntity<?> result = bookController.updateBook(99L, updatedDTO);

        assertEquals(404, result.getStatusCode().value());
        verify(bookService, times(1)).updateBook(99L, updatedDTO);
    }

    @Test
    void givenBlankTitle_whenUpdateBook_thenReturnsBadRequest() {
        BookDTO updatedDTO = buildDTO(1L, "");
        when(bookService.updateBook(1L, updatedDTO))
                .thenThrow(new IllegalArgumentException("Titel mag niet leeg zijn"));

        ResponseEntity<?> result = bookController.updateBook(1L, updatedDTO);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Titel mag niet leeg zijn", result.getBody());
        verify(bookService, times(1)).updateBook(1L, updatedDTO);
    }

    @Test
    void givenNegativePageCount_whenUpdateBook_thenReturnsBadRequest() {
        BookDTO updatedDTO = new BookDTO(1L, "Clean Code", List.of("Author"), "Publisher", "Description",
                -1, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890", 2023, false, null, "A", 1, 1,
                "Eerste graad", null, null);
        when(bookService.updateBook(1L, updatedDTO))
                .thenThrow(new IllegalArgumentException("Paginacount mag niet negatief zijn"));

        ResponseEntity<?> result = bookController.updateBook(1L, updatedDTO);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Paginacount mag niet negatief zijn", result.getBody());
        verify(bookService, times(1)).updateBook(1L, updatedDTO);
    }

    @Test
    void givenFuturePublishedYear_whenUpdateBook_thenReturnsBadRequest() {
        BookDTO updatedDTO = new BookDTO(1L, "Clean Code", List.of("Author"), "Publisher", "Description",
                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890", 9999, false, null, "A", 1, 1,
                "Eerste graad", null, null);
        when(bookService.updateBook(1L, updatedDTO))
                .thenThrow(new IllegalArgumentException("Publicatiejaar mag niet in de toekomst liggen"));

        ResponseEntity<?> result = bookController.updateBook(1L, updatedDTO);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Publicatiejaar mag niet in de toekomst liggen", result.getBody());
        verify(bookService, times(1)).updateBook(1L, updatedDTO);
    }

    @Test
    void givenBookDoesNotExist_whenUpdateBook_thenReturnsNotFoundWithMessage() {
        BookDTO updatedDTO = buildDTO(99L, "Some Title");
        when(bookService.updateBook(99L, updatedDTO)).thenThrow(new BookNotFoundException(99L));

        ResponseEntity<?> result = bookController.updateBook(99L, updatedDTO);

        assertEquals(404, result.getStatusCode().value());
        assertEquals("Book with id 99 could not be found", result.getBody());
        verify(bookService, times(1)).updateBook(99L, updatedDTO);
    }

    @Test
    void givenServiceIsCalled_whenUpdateBook_thenDelegatesOnlyToService() {
        BookDTO updatedDTO = buildDTO(1L, "Clean Code");
        when(bookService.updateBook(1L, updatedDTO)).thenReturn(updatedDTO);

        bookController.updateBook(1L, updatedDTO);

        verify(bookService, times(1)).updateBook(1L, updatedDTO);
        verifyNoMoreInteractions(bookService);
    }

    @Test
    void givenBooksExist_whenGetAllBooksUnpaged_thenReturnsAllDTOs() {
        List<BookDTO> expected = List.of(buildDTO(1L, "Clean Code"), buildDTO(2L, "Effective Java"));
        when(bookService.getAllBooksUnpaged(UserRoles.STUDENT, null)).thenReturn(expected);

        List<BookDTO> result = bookController.getAllBooksUnpaged(null);

        assertEquals(expected, result);
        verify(bookService, times(1)).getAllBooksUnpaged(UserRoles.STUDENT, null);
    }

    @Test
    void givenNoBooksExist_whenGetAllBooksUnpaged_thenReturnsEmptyList() {
        when(bookService.getAllBooksUnpaged(UserRoles.STUDENT, null)).thenReturn(List.of());

        List<BookDTO> result = bookController.getAllBooksUnpaged(null);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getAllBooksUnpaged(UserRoles.STUDENT, null);
    }

    @Test
    void givenServiceFails_whenGetAllBooksUnpaged_thenThrowsException() {
        when(bookService.getAllBooksUnpaged(UserRoles.STUDENT, null))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class, () -> bookController.getAllBooksUnpaged(null));
        verify(bookService, times(1)).getAllBooksUnpaged(UserRoles.STUDENT, null);
    }

    @Test
    void givenServiceIsCalled_whenGetAllBooksUnpaged_thenDelegatesOnlyToService() {
        when(bookService.getAllBooksUnpaged(UserRoles.STUDENT, null)).thenReturn(List.of());

        bookController.getAllBooksUnpaged(null);

        verify(bookService, times(1)).getAllBooksUnpaged(UserRoles.STUDENT, null);
        verifyNoMoreInteractions(bookService);
    }


    @Test
    void updateBook_shouldHaveExpectedPreAuthorizeRule() throws NoSuchMethodException {
        Method method = BookController.class.getMethod("updateBook", Long.class, edu.ap.gosmartlib.dto.BookDTO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasAnyRole('BIBLIOTHEEKBEHEERDER', 'ADMIN')", preAuthorize.value());
    }

    @Test
    void updateSpotlight_shouldHaveExpectedPreAuthorizeRule() throws NoSuchMethodException {
        Method method = BookController.class.getMethod("updateSpotlight", Long.class, boolean.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')", preAuthorize.value());
    }

    @Test
    void givenValidManualBookRequest_whenAddManualBook_thenReturnsCreatedBook() {
        OAuth2User principal = mockPrincipal("uid-123");

        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book", List.of("Author One", "Author Two"), "Manual Publisher", "Manual Description",
                321, List.of("Fantasy", "Young adult"), "thumbnail-url", "nl", 4.5, 2024,
                false, false, null, "A", 1, 1, "Eerste graad", null);

        BookDTO createdBook = new BookDTO(42L, "Manual Book", List.of("Author One", "Author Two"),
                "Manual Publisher", "Manual Description", 321, List.of("Fantasy", "Young adult"),
                "thumbnail-url", "nl", 4.5, "NOISBN-123e4567-e89b-12d3-a456-426614174000",
                2024, false, null, "A", 1, 1, "Eerste graad", null, null);

        when(bookService.addManualBook(request, "uid-123")).thenReturn(createdBook);

        ResponseEntity<?> result = bookController.addManualBook(request, principal);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(createdBook, result.getBody());
        verify(bookService, times(1)).addManualBook(request, "uid-123");
    }

    @Test
    void givenBlankTitle_whenAddManualBook_thenReturnsBadRequest() {
        OAuth2User principal = mockPrincipal("uid-123");

        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "   ", List.of("Author One"), "Publisher", "Description", 100,
                List.of("Fantasy"), "thumbnail-url", "nl", 4.0, 2024,
                false, false, null, "A", 1, 1, "Eerste graad", null);

        when(bookService.addManualBook(request, "uid-123"))
                .thenThrow(new IllegalArgumentException("Titel is verplicht"));

        ResponseEntity<?> result = bookController.addManualBook(request, principal);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Titel is verplicht", result.getBody());
        verify(bookService, times(1)).addManualBook(request, "uid-123");
    }

    @Test
    void givenUnexpectedServiceError_whenAddManualBook_thenReturnsInternalServerError() {
        OAuth2User principal = mockPrincipal("uid-123");

        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book", List.of("Author One"), "Publisher", "Description", 100,
                List.of("Fantasy"), "thumbnail-url", "nl", 4.0, 2024,
                false, false, null, "A", 1, 1, "Eerste graad", null);

        when(bookService.addManualBook(request, "uid-123")).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> result = bookController.addManualBook(request, principal);

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while saving the book.", result.getBody());
        verify(bookService, times(1)).addManualBook(request, "uid-123");
    }

    @Test
    void givenValidIsbn_whenAddBookByIsbn_thenReturnsCreatedBook() {
        OAuth2User principal = mockPrincipal("uid-123");

        BookDTO createdBook = buildDTO(5L, "Clean Code");
        when(bookService.addBookByIsbn("9780132350884", "uid-123", "Campus Zuid", null)).thenReturn(createdBook);

        ResponseEntity<?> result = bookController.addBookByIsbn("9780132350884", "Campus Zuid", null, principal);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(createdBook, result.getBody());
        verify(bookService, times(1)).addBookByIsbn("9780132350884", "uid-123", "Campus Zuid", null);
    }

    @Test
    void givenUnknownIsbn_whenAddBookByIsbn_thenReturnsNotFound() {
        OAuth2User principal = mockPrincipal("uid-123");

        when(bookService.addBookByIsbn("0000000000000", "uid-123", "Campus Zuid", null))
                .thenThrow(new IllegalArgumentException("Geen boek voor ISBN: 0000000000000", null));

        ResponseEntity<?> result = bookController.addBookByIsbn("0000000000000", "Campus Zuid", null, principal);

        assertEquals(404, result.getStatusCode().value());
        assertEquals("Geen boek voor ISBN: 0000000000000", result.getBody());
        verify(bookService, times(1)).addBookByIsbn("0000000000000", "uid-123", "Campus Zuid", null);
    }

    @Test
    void givenUnexpectedServiceError_whenAddBookByIsbn_thenReturnsInternalServerError() {
        OAuth2User principal = mockPrincipal("uid-123");

        when(bookService.addBookByIsbn("9780132350884", "uid-123", "Campus Zuid", null))
                .thenThrow(new RuntimeException("Google API down"));

        ResponseEntity<?> result = bookController.addBookByIsbn("9780132350884", "Campus Zuid", null, principal);

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while fetching the book.", result.getBody());
        verify(bookService, times(1)).addBookByIsbn("9780132350884", "uid-123", "Campus Zuid", null);
    }

    @Test
    void givenValidIsbn_whenSearchBookByIsbn_thenReturnsPreviewBook() {
        BookDTO previewBook = buildDTO(99L, "Preview Book");
        when(bookService.searchBookByIsbn("9780132350884")).thenReturn(previewBook);

        ResponseEntity<?> result = bookController.searchBookByIsbn("9780132350884");

        assertEquals(200, result.getStatusCode().value());
        assertEquals(previewBook, result.getBody());
        verify(bookService, times(1)).searchBookByIsbn("9780132350884");
    }

    @Test
    void givenUnknownIsbn_whenSearchBookByIsbn_thenReturnsNotFound() {
        when(bookService.searchBookByIsbn("0000000000000"))
                .thenThrow(new IllegalArgumentException("Geen boek voor ISBN: 0000000000000"));

        ResponseEntity<?> result = bookController.searchBookByIsbn("0000000000000");

        assertEquals(404, result.getStatusCode().value());
        assertEquals("Geen boek voor ISBN: 0000000000000", result.getBody());
        verify(bookService, times(1)).searchBookByIsbn("0000000000000");
    }

    @Test
    void givenUnexpectedServiceError_whenSearchBookByIsbn_thenReturnsInternalServerError() {
        when(bookService.searchBookByIsbn("9780132350884")).thenThrow(new RuntimeException("Google API down"));

        ResponseEntity<?> result = bookController.searchBookByIsbn("9780132350884");

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while fetching the book.", result.getBody());
        verify(bookService, times(1)).searchBookByIsbn("9780132350884");
    }

    @Test
    void givenBookWithInventories_whenGetBookById_thenReturnsInventoryData() throws BookNotFoundException {
        BookDTO expected = new BookDTO(1L, "Clean Code", List.of("Author"), "Publisher", "Description",
                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890",
                2023, false, List.of("STEM"), "A", 5, 3, "Eerste graad", null, List.of(
                buildInventoryDTO(11L, 1L, "AP Hogeschool", "Campus Noord", 2, 1),
                buildInventoryDTO(12L, 2L, "GO! School", "Campus Zuid", 3, 2)));

        when(bookService.getBookById(1L, UserRoles.STUDENT, null)).thenReturn(expected);

        ResponseEntity<?> result = bookController.getBookById(1L, null);

        assertEquals(200, result.getStatusCode().value());
        assertInstanceOf(BookDTO.class, result.getBody());

        BookDTO body = (BookDTO) result.getBody();
        assertNotNull(body);
        assertEquals(5, body.totalCopies());
        assertEquals(3, body.availableCopies());
        assertNotNull(body.inventories());
        assertEquals(2, body.inventories().size());
        assertEquals("Campus Noord", body.inventories().get(0).campus());
        assertEquals(2, body.inventories().get(0).totalCopies());
        assertEquals("GO! School", body.inventories().get(1).schoolName());
        assertEquals(2, body.inventories().get(1).availableCopies());

        verify(bookService).getBookById(1L, UserRoles.STUDENT, null);
    }

    @Test
    void givenBookWithInventories_whenUpdateBook_thenReturnsUpdatedInventoryData() {
        BookDTO updatedDTO = new BookDTO(1L, "Updated Title", List.of("Author"), "Publisher", "Description",
                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890",
                2023, false, List.of("STEM"), "A", 10, 7, "Eerste graad", null, List.of(
                buildInventoryDTO(null, 1L, "AP Hogeschool", "Campus A", 4, 3),
                buildInventoryDTO(null, 2L, "GO! School", "Campus B", 6, 4)));

        when(bookService.updateBook(1L, updatedDTO)).thenReturn(updatedDTO);

        ResponseEntity<?> result = bookController.updateBook(1L, updatedDTO);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(updatedDTO, result.getBody());

        BookDTO body = (BookDTO) result.getBody();
        assertNotNull(body);
        assertEquals(10, body.totalCopies());
        assertEquals(7, body.availableCopies());
        assertNotNull(body.inventories());
        assertEquals(2, body.inventories().size());
        assertEquals(1L, body.inventories().get(0).schoolId());
        assertEquals("Campus B", body.inventories().get(1).campus());

        verify(bookService).updateBook(1L, updatedDTO);
    }

    @Test
    void givenInvalidInventoryCounts_whenUpdateBook_thenReturnsBadRequest() {
        BookDTO updatedDTO = new BookDTO(1L, "Updated Title", List.of("Author"), "Publisher", "Description",
                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890",
                2023, false, List.of("STEM"), "A", 3, 5, "Eerste graad", null, List.of(
                buildInventoryDTO(null, 1L, "AP Hogeschool", "Campus A", 3, 5)));

        when(bookService.updateBook(1L, updatedDTO))
                .thenThrow(new IllegalArgumentException(
                        "Beschikbare exemplaren mogen niet groter zijn dan totaal aantal exemplaren"));

        ResponseEntity<?> result = bookController.updateBook(1L, updatedDTO);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Beschikbare exemplaren mogen niet groter zijn dan totaal aantal exemplaren", result.getBody());
        verify(bookService).updateBook(1L, updatedDTO);
    }

    @Test
    void givenManualBookRequestWithInventories_whenAddManualBook_thenReturnsCreatedBookWithInventories() {
        OAuth2User principal = mockPrincipal("uid-123");

        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book",
                List.of("Author One", "Author Two"),
                "Manual Publisher",
                "Manual Description",
                321,
                List.of("Fantasy", "Young adult"),
                "thumbnail-url",
                "nl",
                4.5,
                2024,
                false,
                false,
                List.of("STEM"),
                "A",
                5,
                3,
                "Eerste graad",
                List.of(
                        buildCreateInventoryRequest(1L, "Campus Noord", 2, 1),
                        buildCreateInventoryRequest(2L, "Campus Zuid", 3, 2)));

        BookDTO createdBook = new BookDTO(42L, "Manual Book", List.of("Author One", "Author Two"),
                "Manual Publisher", "Manual Description", 321, List.of("Fantasy", "Young adult"),
                "thumbnail-url", "nl", 4.5, "NOISBN-123e4567-e89b-12d3-a456-426614174000",
                2024, false, List.of("STEM"), "A", 5, 3, "Eerste graad", null, List.of(
                buildInventoryDTO(21L, 1L, "AP Hogeschool", "Campus Noord", 2, 1),
                buildInventoryDTO(22L, 2L, "GO! School", "Campus Zuid", 3, 2)));

        when(bookService.addManualBook(request, "uid-123")).thenReturn(createdBook);

        ResponseEntity<?> result = bookController.addManualBook(request, principal);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(createdBook, result.getBody());

        BookDTO body = (BookDTO) result.getBody();
        assertNotNull(body);
        assertEquals(5, body.totalCopies());
        assertEquals(3, body.availableCopies());
        assertNotNull(body.inventories());
        assertEquals(2, body.inventories().size());
        assertEquals("Campus Noord", body.inventories().get(0).campus());
        assertEquals(3, body.inventories().get(1).totalCopies());

        verify(bookService).addManualBook(request, "uid-123");
    }

    @Test
    void givenInvalidInventoryCounts_whenAddManualBook_thenReturnsBadRequest() {
        OAuth2User principal = mockPrincipal("uid-123");

        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "Manual Book",
                List.of("Author One"),
                "Publisher",
                "Description",
                100,
                List.of("Fantasy"),
                "thumbnail-url",
                "nl",
                4.0,
                2024,
                false,
                false,
                List.of("STEM"),
                "A",
                3,
                5,
                "Eerste graad",
                List.of(buildCreateInventoryRequest(1L, "Campus Zuid", 3, 5)));

        when(bookService.addManualBook(request, "uid-123"))
                .thenThrow(new IllegalArgumentException(
                        "Beschikbare exemplaren mogen niet groter zijn dan totaal aantal exemplaren"));

        ResponseEntity<?> result = bookController.addManualBook(request, principal);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Beschikbare exemplaren mogen niet groter zijn dan totaal aantal exemplaren", result.getBody());
        verify(bookService).addManualBook(request, "uid-123");
    }

    // -- helper
    private Page<BookDTO> toPage(List<BookDTO> list) {
        return new PageImpl<>(list, PageRequest.of(0, 20), list.size());
    }

    private OAuth2User mockPrincipal(String uid) {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("userID")).thenReturn(uid);
        return principal;
    }

    private BookInventoryDTO buildInventoryDTO(Long id, Long schoolId, String schoolName, String campus,
                                               Integer totalCopies, Integer availableCopies) {
        return new BookInventoryDTO(id, schoolId, schoolName, campus, totalCopies, availableCopies);
    }

    private CreateBookInventoryRequestDTO buildCreateInventoryRequest(Long schoolId, String campus,
                                                                      Integer totalCopies, Integer availableCopies) {
        return new CreateBookInventoryRequestDTO(schoolId, campus, totalCopies, availableCopies);
    }
}