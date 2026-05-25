package edu.ap.gosmartlib.util;

import edu.ap.gosmartlib.entities.bookEntities.BookEntity;
<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import edu.ap.gosmartlib.repositories.bookRepositories.BookRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeedingTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private DataSeeding dataSeeding;

    @Test
    void givenEmptyDatabase_whenRun_thenSeedsConfiguredBooks() {
        when(bookRepository.count()).thenReturn(0L);

        when(schoolRepository.findByDomain(anyString())).thenReturn(Optional.empty());
        when(schoolRepository.save(any(SchoolEntity.class))).thenAnswer(invocation -> {
            SchoolEntity school = invocation.getArgument(0);
            school.setId(1L);
            return school;
        });

        dataSeeding.run();

        ArgumentCaptor<List<BookEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookRepository).saveAll(captor.capture());
        assertEquals(181, captor.getValue().size());
    }

    @Test
    void givenDatabaseAlreadyHasData_whenRun_thenDoesNotSeed() {
        when(bookRepository.count()).thenReturn(1L);

        dataSeeding.run();

        verify(bookRepository, never()).saveAll(anyList());
    }
}