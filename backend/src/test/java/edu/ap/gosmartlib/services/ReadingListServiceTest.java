package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListResponseDTO;
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
import java.util.ArrayList;
import java.util.HashSet;
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

    //-------------------------------------------------------------------------------------

    @Test
    void syncBooksInList_Success() {
        // 1. ARRANGE: Bereid de testdata voor
        Long listId = 1L;
        List<Long> bookIdsToAdd = List.of(10L, 20L); // De ID's van de boeken die we willen toevoegen

        // Maak een neppe bestaande leeslijst
        ReadingListEntity existingList = new ReadingListEntity();
        existingList.setId(listId);
        existingList.setTitle("Bestaande Lijst");
        existingList.setDeadline(LocalDateTime.now());
        // GEWIJZIGD: Gebruik een HashSet in plaats van een ArrayList!
        existingList.setBooks(new HashSet<>()); 

        // Maak neppe boeken die "gevonden" worden in de database
        BookEntity book1 = new BookEntity(); 
        book1.setId(10L); 
        book1.setTitle("Harry Potter");

        BookEntity book2 = new BookEntity(); 
        book2.setId(20L); 
        book2.setTitle("Lord of the Rings");

        // Vertel de mock-repositories wat ze moeten antwoorden
        when(readingListRepository.findById(listId)).thenReturn(Optional.of(existingList));
        when(bookRepository.findAllById(bookIdsToAdd)).thenReturn(List.of(book1, book2));
        
        // Zorg dat de save() methode gewoon het object teruggeeft dat hij binnenkrijgt
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT: Voer de daadwerkelijke sync-methode uit (boeken toevoegen)
        ReadingListResponseDTO resultDTO = readingListService.syncBooksInList(listId, bookIdsToAdd);

        // 3. ASSERT: Controleer of alles perfect is verlopen
        assertNotNull(resultDTO); // De methode moet een DTO teruggeven
        assertEquals(listId, resultDTO.getId());
        assertEquals(2, resultDTO.getBooks().size()); // Er moeten nu 2 boeken in zitten!

        // Vang het object af dat daadwerkelijk naar de database gestuurd is om op te slaan
        ArgumentCaptor<ReadingListEntity> captor = ArgumentCaptor.forClass(ReadingListEntity.class);
        verify(readingListRepository).save(captor.capture());
        
        ReadingListEntity savedList = captor.getValue();
        assertEquals(2, savedList.getBooks().size());
        assertTrue(savedList.getBooks().contains(book1)); // Controleer of boek 1 is toegevoegd
        assertTrue(savedList.getBooks().contains(book2)); // Controleer of boek 2 is toegevoegd
    }

    @Test
    void syncBooksInList_ListNotFound_ThrowsException() {
        // 1. ARRANGE: Stel dat de hacker/gebruiker een leeslijst ID doorgeeft dat niet bestaat
        Long fakeListId = 999L;
        when(readingListRepository.findById(fakeListId)).thenReturn(Optional.empty());

        // 2 & 3. ACT & ASSERT: Controleren of hij keurig de juiste foutmelding gooit
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            readingListService.syncBooksInList(fakeListId, List.of(10L));
        });

        assertEquals("Leeslijst niet gevonden in DB met ID: 999", exception.getMessage());
        
        // Controleer of hij uit veiligheid nooit heeft geprobeerd boeken te zoeken of op te slaan
        verify(bookRepository, never()).findAllById(any());
        verify(readingListRepository, never()).save(any());
    }

    //-------------------------------------------------------------------------------------

    @Test
    void syncBooksInList_RemoveBook_Success() {
        // 1. ARRANGE
        Long listId = 1L;
        
        // Stel: Boek 10 en Boek 20 zitten momenteel in de leeslijst.
        BookEntity book1 = new BookEntity(); 
        book1.setId(10L); 
        book1.setTitle("Harry Potter");

        BookEntity book2 = new BookEntity(); 
        book2.setId(20L); 
        book2.setTitle("Lord of the Rings");

        ReadingListEntity existingList = new ReadingListEntity();
        existingList.setId(listId);
        existingList.setTitle("Bestaande Lijst");
        existingList.setDeadline(LocalDateTime.now());
        
        // We vullen de bestaande lijst vooraf met BEIDE boeken
        existingList.setBooks(new HashSet<>(List.of(book1, book2))); 

        // De gebruiker klikt boek 20 weg en slaat op. Hij stuurt dus alleen ID 10 nog door.
        List<Long> newBookIds = List.of(10L); 

        // Repositories instellen
        when(readingListRepository.findById(listId)).thenReturn(Optional.of(existingList));
        // De bookRepository zal nu alleen boek 10 nog terugvinden en toevoegen
        when(bookRepository.findAllById(newBookIds)).thenReturn(List.of(book1));
        
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        ReadingListResponseDTO resultDTO = readingListService.syncBooksInList(listId, newBookIds);

        // 3. ASSERT
        assertNotNull(resultDTO);
        assertEquals(1, resultDTO.getBooks().size()); // Er mag er nu maar 1 overblijven in de DTO

        ArgumentCaptor<ReadingListEntity> captor = ArgumentCaptor.forClass(ReadingListEntity.class);
        verify(readingListRepository).save(captor.capture());
        
        ReadingListEntity savedList = captor.getValue();
        assertEquals(1, savedList.getBooks().size()); // Er mag er ook maar 1 overblijven in de database
        assertTrue(savedList.getBooks().contains(book1)); // Boek 10 moet er nog in zitten
        assertFalse(savedList.getBooks().contains(book2)); // Boek 20 moet succesvol verwijderd zijn!
    }

    @Test
    void syncBooksInList_RemoveAllBooks_Success() {
        // 1. ARRANGE
        Long listId = 1L;
        
        ReadingListEntity existingList = new ReadingListEntity();
        existingList.setId(listId);
        // De lijst bevat momenteel 1 boek (mockBook wordt in de @BeforeEach aangemaakt)
        existingList.setBooks(new HashSet<>(List.of(mockBook))); 

        // De gebruiker verwijdert alles en stuurt een lege lijst door
        List<Long> emptyBookIds = new ArrayList<>(); 

        when(readingListRepository.findById(listId)).thenReturn(Optional.of(existingList));
        when(bookRepository.findAllById(emptyBookIds)).thenReturn(new ArrayList<>());
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        ReadingListResponseDTO resultDTO = readingListService.syncBooksInList(listId, emptyBookIds);

        // 3. ASSERT
        assertNotNull(resultDTO);
        assertEquals(0, resultDTO.getBooks().size()); // DTO moet een lege lijst tonen

        ArgumentCaptor<ReadingListEntity> captor = ArgumentCaptor.forClass(ReadingListEntity.class);
        verify(readingListRepository).save(captor.capture());
        
        // Controleer of de database entiteit nu ook echt 0 boeken bevat
        assertEquals(0, captor.getValue().getBooks().size()); 
    }
}