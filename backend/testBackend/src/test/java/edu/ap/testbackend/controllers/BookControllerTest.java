package edu.ap.testbackend.controllers;

import edu.ap.testbackend.services.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                100, List.of("Category"), "thumbnail", "en", 4.0);
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
                buildDTO(3L, "Refactoring")
        );
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
}