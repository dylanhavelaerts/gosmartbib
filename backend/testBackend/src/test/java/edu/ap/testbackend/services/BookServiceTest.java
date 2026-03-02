package edu.ap.testbackend.services;

import edu.ap.testbackend.controllers.BookDTO;
import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.repositories.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private BookEntity sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new BookEntity(
                "Clean Code",
                List.of("Robert C. Martin"),
                "Prentice Hall",
                "A handbook of agile software craftsmanship",
                431,
                List.of("Programming"),
                "http://thumbnail.url/cleancode.jpg",
                "en",
                4.5
        );
        sampleBook.setId(1L);
    }

    // --- getBookById ---

    @Test
    void getBookById_existingId_returnsDTO() throws BookNotFoundException {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        BookDTO result = bookService.getBookById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Clean Code");
    }

    @Test
    void getBookById_nonExistingId_throwsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getBookById_nonExistingId_repositoryFindByIdCalledOnce() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L));

        verify(bookRepository, times(1)).findById(99L);
    }

    // --- deleteBook ---

    @Test
    void deleteBook_existingId_deletesSuccessfully() throws BookNotFoundException {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        bookService.deleteBook(1L);

        verify(bookRepository, times(1)).delete(sampleBook);
    }

    @Test
    void deleteBook_nonExistingId_throwsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteBook_nonExistingId_repositoryDeleteNeverCalled() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(99L));

        verify(bookRepository, never()).delete(any(BookEntity.class));
    }
}