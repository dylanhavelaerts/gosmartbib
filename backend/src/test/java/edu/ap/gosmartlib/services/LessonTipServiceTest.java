package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.lessontip.CreateLessonTipDTO;
import edu.ap.gosmartlib.dto.lessontip.LessonTipDTO;
import edu.ap.gosmartlib.dto.userdirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.entities.LessonTipEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.LessonTipRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonTipServiceTest {

    @Mock private LessonTipRepository lessonTipRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserDirectoryService userDirectoryService;

    @InjectMocks private LessonTipService lessonTipService;

    private static final String ACTOR_UID = "teacher-uid";
    private static final Long BOOK_ID = 1L;

    private UserEntity user;
    private BookEntity book;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(10L);
        user.setSmartschoolUid(ACTOR_UID);

        book = new BookEntity();
        book.setId(BOOK_ID);
        book.setTitle("Test Book");
    }

    // --- getByBookId ---

    @Test
    void givenNoTipsForBook_whenGetByBookId_thenReturnsEmptyList() {
        when(lessonTipRepository.findByBook_IdOrderByCreatedDateDesc(BOOK_ID)).thenReturn(List.of());

        List<LessonTipDTO> result = lessonTipService.getByBookId(BOOK_ID, ACTOR_UID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(userDirectoryService);
    }

    @Test
    void givenAnonymousTip_whenGetByBookId_thenReturnsAnonAuthorWithoutResolvingNames() {
        LessonTipEntity tip = buildTip(1L, user, "Tip tekst", true);
        when(lessonTipRepository.findByBook_IdOrderByCreatedDateDesc(BOOK_ID)).thenReturn(List.of(tip));

        List<LessonTipDTO> result = lessonTipService.getByBookId(BOOK_ID, ACTOR_UID);

        assertEquals(1, result.size());
        assertEquals("Anoniem", result.get(0).authorName());
        assertTrue(result.get(0).ownTip());
        verifyNoInteractions(userDirectoryService);
    }

    @Test
    void givenNonAnonymousTip_whenGetByBookId_thenResolvesDisplayName() {
        LessonTipEntity tip = buildTip(1L, user, "Tip tekst", false);
        when(lessonTipRepository.findByBook_IdOrderByCreatedDateDesc(BOOK_ID)).thenReturn(List.of(tip));
        when(userDirectoryService.resolveDisplayNames(eq(ACTOR_UID), any()))
                .thenReturn(new ResolveDisplayNamesResponse(true, 1, 1, Map.of(ACTOR_UID, "Jan Janssen"), List.of(), null));

        List<LessonTipDTO> result = lessonTipService.getByBookId(BOOK_ID, ACTOR_UID);

        assertEquals(1, result.size());
        assertEquals("Jan Janssen", result.get(0).authorName());
        assertTrue(result.get(0).ownTip());
    }

    // --- create ---

    @Test
    void givenValidRequest_whenCreate_thenSavesAndReturnsTip() {
        CreateLessonTipDTO dto = new CreateLessonTipDTO("  Mijn tip  ", false);
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.of(user));
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(lessonTipRepository.save(any())).thenAnswer(inv -> {
            LessonTipEntity t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });
        when(userDirectoryService.resolveDisplayNames(eq(ACTOR_UID), any()))
                .thenReturn(new ResolveDisplayNamesResponse(true, 1, 1, Map.of(ACTOR_UID, "Jan"), List.of(), null));

        LessonTipDTO result = lessonTipService.create(BOOK_ID, ACTOR_UID, dto);

        assertEquals("Mijn tip", result.text());
        assertFalse(result.anonymous());
        assertTrue(result.ownTip());
        verify(lessonTipRepository).save(any());
    }

    @Test
    void givenUserNotFound_whenCreate_thenThrows401() {
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lessonTipService.create(BOOK_ID, ACTOR_UID, new CreateLessonTipDTO("tip", false)));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(lessonTipRepository, never()).save(any());
    }

    @Test
    void givenBookNotFound_whenCreate_thenThrowsBookNotFoundException() {
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.of(user));
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class,
                () -> lessonTipService.create(BOOK_ID, ACTOR_UID, new CreateLessonTipDTO("tip", false)));

        verify(lessonTipRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void givenOwnTip_whenUpdate_thenUpdatesAndReturnsTip() {
        LessonTipEntity tip = buildTip(5L, user, "Oude tekst", false);
        CreateLessonTipDTO dto = new CreateLessonTipDTO("  Nieuwe tekst  ", true);
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.of(user));
        when(lessonTipRepository.findById(5L)).thenReturn(Optional.of(tip));
        when(lessonTipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LessonTipDTO result = lessonTipService.update(5L, ACTOR_UID, dto);

        assertEquals("Nieuwe tekst", result.text());
        assertTrue(result.anonymous());
        assertEquals("Anoniem", result.authorName());
    }

    @Test
    void givenOtherUsersTip_whenUpdate_thenThrowsAccessDeniedException() {
        UserEntity otherUser = new UserEntity();
        otherUser.setId(99L);
        otherUser.setSmartschoolUid("other-uid");

        LessonTipEntity tip = buildTip(5L, otherUser, "Tekst", false);
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.of(user));
        when(lessonTipRepository.findById(5L)).thenReturn(Optional.of(tip));

        assertThrows(AccessDeniedException.class,
                () -> lessonTipService.update(5L, ACTOR_UID, new CreateLessonTipDTO("nieuw", false)));

        verify(lessonTipRepository, never()).save(any());
    }

    @Test
    void givenTipNotFound_whenUpdate_thenThrows404() {
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.of(user));
        when(lessonTipRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lessonTipService.update(99L, ACTOR_UID, new CreateLessonTipDTO("tekst", false)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- delete ---

    @Test
    void givenOwnTip_whenDelete_thenDeletesTip() {
        LessonTipEntity tip = buildTip(7L, user, "Te verwijderen", false);
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.of(user));
        when(lessonTipRepository.findById(7L)).thenReturn(Optional.of(tip));

        lessonTipService.delete(7L, ACTOR_UID);

        verify(lessonTipRepository).delete(tip);
    }

    @Test
    void givenOtherUsersTip_whenDelete_thenThrowsAccessDeniedException() {
        UserEntity otherUser = new UserEntity();
        otherUser.setId(99L);
        LessonTipEntity tip = buildTip(7L, otherUser, "Tekst", false);
        when(userRepository.findBySmartschoolUid(ACTOR_UID)).thenReturn(Optional.of(user));
        when(lessonTipRepository.findById(7L)).thenReturn(Optional.of(tip));

        assertThrows(AccessDeniedException.class, () -> lessonTipService.delete(7L, ACTOR_UID));

        verify(lessonTipRepository, never()).delete(any());
    }

    private LessonTipEntity buildTip(Long id, UserEntity author, String text, boolean anonymous) {
        LessonTipEntity tip = new LessonTipEntity();
        tip.setId(id);
        tip.setUser(author);
        tip.setBook(book);
        tip.setText(text);
        tip.setAnonymous(anonymous);
        tip.setCreatedDate(LocalDate.of(2024, 1, 15));
        return tip;
    }
}
