package edu.ap.gosmartlib.util;

import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
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
    void givenEmptyDatabase_whenRun_thenSeedsTwelveBooks() {
        when(bookRepository.count()).thenReturn(0L);

        dataSeeding.run();

        ArgumentCaptor<List<BookEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookRepository).saveAll(captor.capture());
        assertEquals(12, captor.getValue().size());
    }

    @Test
    void givenDatabaseAlreadyHasData_whenRun_thenDoesNotSeed() {
        when(bookRepository.count()).thenReturn(1L);

        dataSeeding.run();

        verify(bookRepository, never()).saveAll(anyList());
    }
}