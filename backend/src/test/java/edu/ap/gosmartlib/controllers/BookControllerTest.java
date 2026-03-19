package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.BookDTO;
import edu.ap.gosmartlib.dto.CreateBookRequestDTO;
import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.dto.importdto.ImportMismatchDTO;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.services.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookController bookController;

    private BookDTO buildDTO(Long id, String title) {
        return new BookDTO(id, title, List.of("Author"), "Publisher", "Description",
                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890",
                2023, false, null, "A", 1, 1);
    }

    @Test
    void givenBooksExist_whenGetBooks_thenReturnsExpectedDTOs() {
        List<BookDTO> books = List.of(buildDTO(1L, "Clean Code"));
        Page<BookDTO> expected = toPage(books);
        when(bookService.getAllBooks(0, 20)).thenReturn(expected);

        Page<BookDTO> result = bookController.getBooks(0, 20);

        assertEquals(expected, result);
        verify(bookService, times(1)).getAllBooks(0, 20);
    }

    @Test
    void givenNoBooksExist_whenGetBooks_thenReturnsEmptyPage() {
        Page<BookDTO> expected = toPage(List.of());
        when(bookService.getAllBooks(0, 20)).thenReturn(expected);

        Page<BookDTO> result = bookController.getBooks(0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(bookService, times(1)).getAllBooks(0, 20);
    }

    @Test
    void givenMultipleBooksExist_whenGetBooks_thenReturnsAllBooks() {
        List<BookDTO> books = List.of(
                buildDTO(1L, "Clean Code"),
                buildDTO(2L, "Effective Java"),
                buildDTO(3L, "Refactoring"));
        Page<BookDTO> expected = toPage(books);
        when(bookService.getAllBooks(0, 20)).thenReturn(expected);

        Page<BookDTO> result = bookController.getBooks(0, 20);

        assertEquals(3, result.getContent().size());
        assertEquals("Clean Code", result.getContent().get(0).title());
        assertEquals("Effective Java", result.getContent().get(1).title());
        assertEquals("Refactoring", result.getContent().get(2).title());
        verify(bookService, times(1)).getAllBooks(0, 20);
    }

    @Test
    void givenServiceFails_whenGetBooks_thenThrowsException() {
        when(bookService.getAllBooks(0, 20)).thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(RuntimeException.class, () -> bookController.getBooks(0, 20));
        verify(bookService, times(1)).getAllBooks(0, 20);
    }

    @Test
    void givenServiceIsCalled_whenGetBooks_thenDelegatesOnlyToService() {
        when(bookService.getAllBooks(0, 20)).thenReturn(toPage(List.of()));

        bookController.getBooks(0, 20);

        verify(bookService, times(1)).getAllBooks(0, 20);
        verifyNoMoreInteractions(bookService);
    }

    @Test
    void givenBookExists_whenGetBookById_thenReturnsCorrectDTO() throws BookNotFoundException {
        BookDTO expected = buildDTO(1L, "Clean Code");
        when(bookService.getBookById(1L)).thenReturn(expected);

        BookDTO result = bookController.getBookById(1L);

        assertEquals(expected, result);
        verify(bookService, times(1)).getBookById(1L);
    }

    @Test
    void givenBookDoesNotExist_whenGetBookById_thenThrowsBookNotFoundException() throws BookNotFoundException {
        when(bookService.getBookById(99L)).thenThrow(new BookNotFoundException(99L));

        assertThrows(BookNotFoundException.class, () -> bookController.getBookById(99L));
        verify(bookService, times(1)).getBookById(99L);
    }

    @Test
    void givenBookExists_whenDeleteBook_thenReturnsNoContent() throws BookNotFoundException {
        ResponseEntity<Void> result = bookController.deleteBook(1L);

        assertEquals(204, result.getStatusCode().value());
        verify(bookService, times(1)).deleteBook(1L);
    }

    @Test
    void givenBookDoesNotExist_whenDeleteBook_thenThrowsBookNotFoundException() throws BookNotFoundException {
        doThrow(new BookNotFoundException(99L)).when(bookService).deleteBook(99L);

        assertThrows(BookNotFoundException.class, () -> bookController.deleteBook(99L));
        verify(bookService, times(1)).deleteBook(99L);
    }

    @Test
    void givenSpotlightBooksExist_whenGetBooksInSpotlight_thenReturnsExpectedDTOs() {
        List<BookDTO> expected = List.of(
                buildDTO(1L, "Spotlight Book 1"),
                buildDTO(2L, "Spotlight Book 2"));
        when(bookService.getTop4BooksInSpotlight()).thenReturn(expected);

        List<BookDTO> result = bookController.getBooksInSpotlight();

        assertEquals(expected, result);
        verify(bookService, times(1)).getTop4BooksInSpotlight();
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetBooksInSpotlight_thenReturnsEmptyList() {
        when(bookService.getTop4BooksInSpotlight()).thenReturn(List.of());

        List<BookDTO> result = bookController.getBooksInSpotlight();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getTop4BooksInSpotlight();
    }

    @Test
    void givenLatestBooksExist_whenGetLatestBooks_thenReturnsExpectedDTOs() {
        List<BookDTO> expected = List.of(
                buildDTO(10L, "Latest Book 1"),
                buildDTO(11L, "Latest Book 2"),
                buildDTO(12L, "Latest Book 3"));
        when(bookService.getLatestBooks()).thenReturn(expected);

        List<BookDTO> result = bookController.getLatestBooks();

        assertEquals(expected, result);
        verify(bookService, times(1)).getLatestBooks();
    }

    @Test
    void givenNoLatestBooksExist_whenGetLatestBooks_thenReturnsEmptyList() {
        when(bookService.getLatestBooks()).thenReturn(List.of());

        List<BookDTO> result = bookController.getLatestBooks();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getLatestBooks();
    }
    // --- filterBooks Controller Tests ---

    @Test
    void givenValidFilters_whenFilterBooks_thenReturnsOk() {
        Page<BookDTO> expected = toPage(List.of(buildDTO(1L, "Clean Code")));
        when(bookService.filterBooks("en", List.of("Programming"), 100, 500, 2000, 2023, 0, 20))
                .thenReturn(expected);

        ResponseEntity<?> result = bookController.filterBooks("en", List.of("Programming"), 100, 500, 2000, 2023, 0,
                20);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).filterBooks("en", List.of("Programming"), 100, 500, 2000, 2023, 0, 20);
    }

    @Test
    void givenNullFilters_whenFilterBooks_thenReturnsOk() {
        when(bookService.filterBooks(null, null, null, null, null, null, 0, 20))
                .thenReturn(toPage(List.of(buildDTO(1L, "Clean Code"))));

        ResponseEntity<?> result = bookController.filterBooks(null, null, null, null, null, null, 0, 20);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void givenMinGreaterThanMax_whenFilterBooks_thenReturnsBadRequest() {
        when(bookService.filterBooks(null, null, 500, 100, null, null, 0, 20))
                .thenThrow(new IllegalArgumentException("minPageCount cannot be bigger than maxPageCount"));

        ResponseEntity<?> result = bookController.filterBooks(null, null, 500, 100, null, null, 0, 20);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("minPageCount cannot be bigger than maxPageCount", result.getBody());
    }

    @Test
    void givenDatabaseFails_whenFilterBooks_thenReturnsInternalServerError() {
        when(bookService.filterBooks(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new DataAccessException("DB down") {
                });

        ResponseEntity<?> result = bookController.filterBooks(null, null, null, null, null, null, 0, 20);

        assertEquals(500, result.getStatusCode().value());
    }

    @Test
    void givenNoResults_whenFilterBooks_thenReturnsEmptyPage() {
        when(bookService.filterBooks(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(toPage(List.of()));

        ResponseEntity<?> result = bookController.filterBooks(null, null, null, null, null, null, 0, 20);

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

        when(bookService.getAllBooksInSpotlight()).thenReturn(expected);

        List<BookDTO> result = bookController.getAllBooksInSpotlight();

        assertEquals(expected, result);
        verify(bookService, times(1)).getAllBooksInSpotlight();
    }

    @Test
    void givenNoSpotlightBooksExist_whenGetAllBooksInSpotlight_thenReturnsEmptyList() {
        when(bookService.getAllBooksInSpotlight()).thenReturn(List.of());

        List<BookDTO> result = bookController.getAllBooksInSpotlight();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getAllBooksInSpotlight();
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
        when(bookService.searchByTitleOrAuthor("Clean", 0, 20)).thenReturn(expected);

        ResponseEntity<Page<BookDTO>> result = bookController.searchByTitleOrAuthor("Clean", 0, 20);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).searchByTitleOrAuthor("Clean", 0, 20);
    }

    @Test
    void givenValidExcelFile_whenImportBooks_thenReturnsOkWithImportSummary() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        BulkImportResponseDTO expected = new BulkImportResponseDTO(
                3,
                2,
                1,
                List.of(new ImportMismatchDTO(4, "9780132350884", "Wrong Title", "Clean Code",
                        "De titel komt niet overeen (Clean Code)")));

        when(bookService.importBooksFromExcel(any(MultipartFile.class))).thenReturn(expected);

        ResponseEntity<?> result = bookController.importBooks(file);

        assertEquals(200, result.getStatusCode().value());
        assertSame(expected, result.getBody());
        verify(bookService, times(1)).importBooksFromExcel(file);
    }

    @Test
    void givenInvalidExcelFile_whenImportBooks_thenReturnsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        when(bookService.importBooksFromExcel(any(MultipartFile.class)))
                .thenThrow(new IllegalArgumentException("Upload een excel file die niet leeg is"));

        ResponseEntity<?> result = bookController.importBooks(file);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Upload een excel file die niet leeg is", result.getBody());
        verify(bookService, times(1)).importBooksFromExcel(file);
    }

    @Test
    void givenUnexpectedServiceError_whenImportBooks_thenReturnsInternalServerError() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        when(bookService.importBooksFromExcel(any(MultipartFile.class)))
                .thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> result = bookController.importBooks(file);

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while importing the Excel file.", result.getBody());
        verify(bookService, times(1)).importBooksFromExcel(file);
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
                -1, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890", 2023, false, null, "A", 1, 1);
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
                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890", 9999, false, null, "A", 1, 1);
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
        when(bookService.updateBook(99L, updatedDTO))
                .thenThrow(new BookNotFoundException(99L));

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
        when(bookService.getAllBooksUnpaged()).thenReturn(expected);

        List<BookDTO> result = bookController.getAllBooksUnpaged();

        assertEquals(expected, result);
        verify(bookService, times(1)).getAllBooksUnpaged();
    }

    @Test
    void givenNoBooksExist_whenGetAllBooksUnpaged_thenReturnsEmptyList() {
        when(bookService.getAllBooksUnpaged()).thenReturn(List.of());

        List<BookDTO> result = bookController.getAllBooksUnpaged();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getAllBooksUnpaged();
    }

    @Test
    void givenServiceFails_whenGetAllBooksUnpaged_thenThrowsException() {
        when(bookService.getAllBooksUnpaged()).thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(RuntimeException.class, () -> bookController.getAllBooksUnpaged());
        verify(bookService, times(1)).getAllBooksUnpaged();
    }

    @Test
    void givenServiceIsCalled_whenGetAllBooksUnpaged_thenDelegatesOnlyToService() {
        when(bookService.getAllBooksUnpaged()).thenReturn(List.of());

        bookController.getAllBooksUnpaged();

        verify(bookService, times(1)).getAllBooksUnpaged();
        verifyNoMoreInteractions(bookService);
    }

    // -- helper
    private Page<BookDTO> toPage(List<BookDTO> list) {
        return new PageImpl<>(list, PageRequest.of(0, 20), list.size());
    }

    @Test
    void givenValidManualBookRequest_whenAddManualBook_thenReturnsCreatedBook() {
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
                false);

        BookDTO createdBook = new BookDTO(
                42L,
                "Manual Book",
                List.of("Author One", "Author Two"),
                "Manual Publisher",
                "Manual Description",
                321,
                List.of("Fantasy", "Young adult"),
                "thumbnail-url",
                "nl",
                4.5,
                "NOISBN-123e4567-e89b-12d3-a456-426614174000",
                2024, false, null, "A", 1, 1);

        when(bookService.addManualBook(request)).thenReturn(createdBook);

        ResponseEntity<?> result = bookController.addManualBook(request);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(createdBook, result.getBody());
        verify(bookService, times(1)).addManualBook(request);
    }

    @Test
    void givenBlankTitle_whenAddManualBook_thenReturnsBadRequest() {
        CreateBookRequestDTO request = new CreateBookRequestDTO(
                "   ",
                List.of("Author One"),
                "Publisher",
                "Description",
                100,
                List.of("Fantasy"),
                "thumbnail-url",
                "nl",
                4.0,
                2024,
                false);

        when(bookService.addManualBook(request))
                .thenThrow(new IllegalArgumentException("Titel is verplicht"));

        ResponseEntity<?> result = bookController.addManualBook(request);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("Titel is verplicht", result.getBody());
        verify(bookService, times(1)).addManualBook(request);
    }

    @Test
    void givenUnexpectedServiceError_whenAddManualBook_thenReturnsInternalServerError() {
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
                false);

        when(bookService.addManualBook(request))
                .thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> result = bookController.addManualBook(request);

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while saving the book.", result.getBody());
        verify(bookService, times(1)).addManualBook(request);
    }

    @Test
    void givenValidIsbn_whenAddBookByIsbn_thenReturnsCreatedBook() {
        BookDTO createdBook = buildDTO(5L, "Clean Code");
        when(bookService.addBookByIsbn("9780132350884")).thenReturn(createdBook);

        ResponseEntity<?> result = bookController.addBookByIsbn("9780132350884");

        assertEquals(201, result.getStatusCode().value());
        assertEquals(createdBook, result.getBody());
        verify(bookService, times(1)).addBookByIsbn("9780132350884");
    }

    @Test
    void givenUnknownIsbn_whenAddBookByIsbn_thenReturnsNotFound() {
        when(bookService.addBookByIsbn("0000000000000"))
                .thenThrow(new IllegalArgumentException("Geen boek voor ISBN: 0000000000000"));

        ResponseEntity<?> result = bookController.addBookByIsbn("0000000000000");

        assertEquals(404, result.getStatusCode().value());
        assertEquals("Geen boek voor ISBN: 0000000000000", result.getBody());
        verify(bookService, times(1)).addBookByIsbn("0000000000000");
    }

    @Test
    void givenUnexpectedServiceError_whenAddBookByIsbn_thenReturnsInternalServerError() {
        when(bookService.addBookByIsbn("9780132350884"))
                .thenThrow(new RuntimeException("Google API down"));

        ResponseEntity<?> result = bookController.addBookByIsbn("9780132350884");

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while fetching the book.", result.getBody());
        verify(bookService, times(1)).addBookByIsbn("9780132350884");
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
        when(bookService.searchBookByIsbn("9780132350884"))
                .thenThrow(new RuntimeException("Google API down"));

        ResponseEntity<?> result = bookController.searchBookByIsbn("9780132350884");

        assertEquals(500, result.getStatusCode().value());
        assertEquals("An error occurred while fetching the book.", result.getBody());
        verify(bookService, times(1)).searchBookByIsbn("9780132350884");
    }
}