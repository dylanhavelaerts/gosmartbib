package edu.ap.testbackend.controllers;

import edu.ap.testbackend.dto.BookDTO;
import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.services.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
                2023);
    }

    @Test
    void givenBooksExist_whenGetBooks_thenReturnsExpectedDTOs() {
        List<BookDTO> expected = List.of(buildDTO(1L, "Clean Code"));
        when(bookService.getAllBooks()).thenReturn(expected);

        List<BookDTO> result = bookController.getBooks();

        assertEquals(expected, result);
        verify(bookService, times(1)).getAllBooks();
    }

    @Test
    void givenNoBooksExist_whenGetBooks_thenReturnsEmptyList() {
        when(bookService.getAllBooks()).thenReturn(List.of());

        List<BookDTO> result = bookController.getBooks();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bookService, times(1)).getAllBooks();
    }

    @Test
    void givenMultipleBooksExist_whenGetBooks_thenReturnsAllBooks() {
        List<BookDTO> expected = List.of(
                buildDTO(1L, "Clean Code"),
                buildDTO(2L, "Effective Java"),
                buildDTO(3L, "Refactoring"));
        when(bookService.getAllBooks()).thenReturn(expected);

        List<BookDTO> result = bookController.getBooks();

        assertEquals(3, result.size());
        assertEquals("Clean Code", result.get(0).title());
        assertEquals("Effective Java", result.get(1).title());
        assertEquals("Refactoring", result.get(2).title());
        verify(bookService, times(1)).getAllBooks();
    }

    @Test
    void givenServiceFails_whenGetBooks_thenThrowsException() {
        when(bookService.getAllBooks()).thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(RuntimeException.class, () -> bookController.getBooks());
        verify(bookService, times(1)).getAllBooks();
    }

    @Test
    void givenServiceIsCalled_whenGetBooks_thenDelegatesOnlyToService() {
        when(bookService.getAllBooks()).thenReturn(List.of());

        bookController.getBooks();

        verify(bookService, times(1)).getAllBooks();
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
        List<BookDTO> expected = List.of(buildDTO(1L, "Clean Code"));
        when(bookService.filterBooks("en", List.of("Programming"), 100, 500, 2000, 2023))
                .thenReturn(expected);

        ResponseEntity<?> result = bookController.filterBooks("en", List.of("Programming"), 100, 500, 2000, 2023);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).filterBooks("en", List.of("Programming"), 100, 500, 2000, 2023);
    }

    @Test
    void givenNullFilters_whenFilterBooks_thenReturnsOk() {
        when(bookService.filterBooks(null, null, null, null, null, null))
                .thenReturn(List.of(buildDTO(1L, "Clean Code")));

        ResponseEntity<?> result = bookController.filterBooks(null, null, null, null, null, null);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void givenMinGreaterThanMax_whenFilterBooks_thenReturnsBadRequest() {
        when(bookService.filterBooks(null, null, 500, 100, null, null))
                .thenThrow(new IllegalArgumentException("minPageCount cannot be bigger than maxPageCount"));

        ResponseEntity<?> result = bookController.filterBooks(null, null, 500, 100, null, null);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("minPageCount cannot be bigger than maxPageCount", result.getBody());
    }

    @Test
    void givenDatabaseFails_whenFilterBooks_thenReturnsInternalServerError() {
        when(bookService.filterBooks(any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataAccessException("DB down") {});

        ResponseEntity<?> result = bookController.filterBooks(null, null, null, null, null, null);

        assertEquals(500, result.getStatusCode().value());
    }

    @Test
    void givenNoResults_whenFilterBooks_thenReturnsEmptyList() {
        when(bookService.filterBooks(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        ResponseEntity<?> result = bookController.filterBooks(null, null, null, null, null, null);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(List.of(), result.getBody());
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
        List<BookDTO> expected = List.of(buildDTO(1L, "Clean Code"));
        when(bookService.searchByTitleOrAuthor("Clean")).thenReturn(expected);

        ResponseEntity<List<BookDTO>> result = bookController.searchByTitleOrAuthor("Clean");

        assertEquals(200, result.getStatusCode().value());
        assertEquals(expected, result.getBody());
        verify(bookService, times(1)).searchByTitleOrAuthor("Clean");
    }

}