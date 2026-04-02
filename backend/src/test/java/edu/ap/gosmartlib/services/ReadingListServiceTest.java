package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingListServiceTest {

    // Mockito maakt 'nep' versies van je repositories
    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    // Mockito injecteert de nep-repositories in jouw échte service
    @InjectMocks
    private ReadingListService readingListService;

    private CreateReadingListDTO dto;
    private UserEntity mockUser;
    private BookEntity mockBook;

    @BeforeEach
    void setUp() {
        // Dit wordt voor ELKE test uitgevoerd, zo starten we met een propere lei
        dto = new CreateReadingListDTO();
        dto.setTitle("Test Lijst");
        dto.setTaskDescription("Lees dit aandachtig voor het examen");
        dto.setDeadline("2026-05-20T12:00:00");
        dto.setCreatorId(1L);
        dto.setBookIds(List.of(10L));

        mockUser = new UserEntity();
        mockUser.setId(1L);

        mockBook = new BookEntity();
        mockBook.setId(10L);
    }

    @Test
    void createReadingList_Success() {
        // 1. ARRANGE (Voorbereiden: Wat moeten de nep-repositories antwoorden?)
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(bookRepository.findAllById(List.of(10L))).thenReturn(List.of(mockBook));
        
        ReadingListEntity savedEntity = new ReadingListEntity();
        savedEntity.setId(100L); // Stel dat hij succesvol wordt opgeslagen en ID 100 krijgt
        when(readingListRepository.save(any(ReadingListEntity.class))).thenReturn(savedEntity);

        // 2. ACT (Uitvoeren van jouw methode)
        ReadingListEntity result = readingListService.createReadingList(dto);

        // 3. ASSERT (Controleren)
        assertNotNull(result); // Mag niet null zijn
        
        // We vangen het object af dat naar de repository.save() werd gestuurd om de inhoud te controleren
        ArgumentCaptor<ReadingListEntity> captor = ArgumentCaptor.forClass(ReadingListEntity.class);
        verify(readingListRepository).save(captor.capture());
        
        ReadingListEntity captured = captor.getValue();
        assertEquals("Test Lijst", captured.getTitle());
        assertEquals("Lees dit aandachtig voor het examen", captured.getTaskDescription());
        assertEquals(LocalDateTime.parse("2026-05-20T12:00:00"), captured.getDeadline());
        assertEquals(mockUser, captured.getCreator());
        assertTrue(captured.getBooks().contains(mockBook));
    }

    @Test
    void createReadingList_UserNotFound_ThrowsException() {
        // 1. ARRANGE: Stel dat de database zegt: "Ik kan deze ID niet vinden"
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // 2 & 3. ACT & ASSERT: Controleren of hij inderdaad keurig crasht met de juiste error
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            readingListService.createReadingList(dto);
        });

        assertEquals("Gebruiker niet gevonden in DB met ID: 1", exception.getMessage());
        
        // Extra controle: Als de gebruiker niet bestaat, mag hij ook nooit proberen om boeken te zoeken of op te slaan!
        verify(bookRepository, never()).findAllById(any());
        verify(readingListRepository, never()).save(any());
    }
    
    @Test
    void createReadingList_WithoutBooks_Success() {
        // 1. ARRANGE: We halen de boeken uit de DTO
        dto.setBookIds(null); 
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(readingListRepository.save(any(ReadingListEntity.class))).thenReturn(new ReadingListEntity());

        // 2. ACT
        readingListService.createReadingList(dto);

        // 3. ASSERT: Controleren of hij het opslaan overleeft zónder dat hij de bookRepository aanroept
        verify(bookRepository, never()).findAllById(any());
        verify(readingListRepository).save(any(ReadingListEntity.class));
    }
}