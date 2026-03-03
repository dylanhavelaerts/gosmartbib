package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.exceptions.BookNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import edu.ap.testbackend.repositories.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

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
    // --- getBookById ---

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

// --- deleteBook ---

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
}