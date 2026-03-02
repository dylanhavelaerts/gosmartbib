package edu.ap.testbackend.util;

import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.repositories.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeedingTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private DataSeeding dataSeeding;

    @Test
    void run_seedsWhenEmpty() {
        when(bookRepository.count()).thenReturn(0L);

        dataSeeding.run();

        ArgumentCaptor<List<BookEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookRepository).saveAll(captor.capture());
        assertEquals(12, captor.getValue().size());
    }

    @Test
    void run_doesNothingWhenNotEmpty() {
        when(bookRepository.count()).thenReturn(1L);

        dataSeeding.run();

        verify(bookRepository, never()).saveAll(anyList());
    }
}
