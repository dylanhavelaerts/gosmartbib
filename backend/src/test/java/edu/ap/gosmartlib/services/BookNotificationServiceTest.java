package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.entities.*;
import edu.ap.gosmartlib.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookNotificationServiceTest {

    @Mock
    private BookNotificationRepository bookNotificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private SmartschoolMessageService messageService;

    @InjectMocks
    private BookNotificationService bookNotificationService;

    // --- enable ---

    @Test
    void givenValidUserAndBook_whenEnable_thenSavesNotification() {
        UserEntity user = buildUser(1L, "uid-1");
        BookEntity book = buildBook(10L, "9780000000001");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookNotificationRepository.existsByUser_IdAndBook_Id(1L, 10L)).thenReturn(false);

        bookNotificationService.enable("uid-1", 10L);

        ArgumentCaptor<BookNotificationEntity> captor = ArgumentCaptor.forClass(BookNotificationEntity.class);
        verify(bookNotificationRepository, times(1)).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
        assertEquals(book, captor.getValue().getBook());
    }

    @Test
    void givenNotificationAlreadyExists_whenEnable_thenDoesNotSaveDuplicate() {
        UserEntity user = buildUser(1L, "uid-1");
        BookEntity book = buildBook(10L, "9780000000001");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookNotificationRepository.existsByUser_IdAndBook_Id(1L, 10L)).thenReturn(true);

        bookNotificationService.enable("uid-1", 10L);

        verify(bookNotificationRepository, never()).save(any());
    }

    @Test
    void givenUserNotFound_whenEnable_thenThrowsNotFound() {
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookNotificationService.enable("uid-1", 10L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenBookNotFound_whenEnable_thenThrowsNotFound() {
        UserEntity user = buildUser(1L, "uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookNotificationService.enable("uid-1", 10L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- disable ---

    @Test
    void givenValidUser_whenDisable_thenDeletesNotification() {
        UserEntity user = buildUser(1L, "uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));

        bookNotificationService.disable("uid-1", 10L);

        verify(bookNotificationRepository, times(1)).deleteByUser_IdAndBook_Id(1L, 10L);
    }

    @Test
    void givenUserNotFound_whenDisable_thenThrowsNotFound() {
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookNotificationService.disable("uid-1", 10L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- isEnabled ---

    @Test
    void givenNotificationExists_whenIsEnabled_thenReturnsTrue() {
        UserEntity user = buildUser(1L, "uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookNotificationRepository.existsByUser_IdAndBook_Id(1L, 10L)).thenReturn(true);

        assertTrue(bookNotificationService.isEnabled("uid-1", 10L));
    }

    @Test
    void givenNoNotification_whenIsEnabled_thenReturnsFalse() {
        UserEntity user = buildUser(1L, "uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookNotificationRepository.existsByUser_IdAndBook_Id(1L, 10L)).thenReturn(false);

        assertFalse(bookNotificationService.isEnabled("uid-1", 10L));
    }

    @Test
    void givenUserNotFound_whenIsEnabled_thenReturnsFalse() {
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.empty());

        assertFalse(bookNotificationService.isEnabled("uid-1", 10L));
    }

    // --- triggerNotificationsForBook ---

    @Test
    void givenNotificationsExist_whenTriggerNotificationsForBook_thenSendsMessageAndDeletesEachRecord() {
        BookEntity book = buildBook(10L, "9780000000001");
        book.setTitle("Clean Code");
        UserEntity user1 = buildUser(1L, "uid-1");
        UserEntity user2 = buildUser(2L, "uid-2");
        BookNotificationEntity n1 = buildNotification(1L, user1, book);
        BookNotificationEntity n2 = buildNotification(2L, user2, book);
        when(bookNotificationRepository.findAllByBook_IdAndUser_School_Id(10L, 5L))
                .thenReturn(List.of(n1, n2));

        bookNotificationService.triggerNotificationsForBook(book, 5L);

        verify(messageService, times(1)).sendMessage(eq(user1), eq("Boek terug beschikbaar"), anyString());
        verify(messageService, times(1)).sendMessage(eq(user2), eq("Boek terug beschikbaar"), anyString());
        verify(bookNotificationRepository, times(1)).delete(n1);
        verify(bookNotificationRepository, times(1)).delete(n2);
    }

    @Test
    void givenNoNotifications_whenTriggerNotificationsForBook_thenSendsNoMessages() {
        BookEntity book = buildBook(10L, "9780000000001");
        when(bookNotificationRepository.findAllByBook_IdAndUser_School_Id(10L, 5L))
                .thenReturn(List.of());

        bookNotificationService.triggerNotificationsForBook(book, 5L);

        verify(messageService, never()).sendMessage(any(), any(), any());
        verify(bookNotificationRepository, never()).delete(any());
    }

    // --- helpers ---

    private UserEntity buildUser(Long id, String uid) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(uid);
        return user;
    }

    private BookEntity buildBook(Long id, String isbn) {
        BookEntity book = new BookEntity();
        book.setId(id);
        book.setIsbn(isbn);
        return book;
    }

    private BookNotificationEntity buildNotification(Long id, UserEntity user, BookEntity book) {
        BookNotificationEntity n = new BookNotificationEntity();
        n.setId(id);
        n.setUser(user);
        n.setBook(book);
        return n;
    }
}
