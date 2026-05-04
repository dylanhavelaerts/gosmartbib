package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.BookNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingListNotificationControllerTest {

    @Mock
    private BookNotificationService bookNotificationService;

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private OAuth2User oAuth2User;

    // @Spy gebruikt de echte implementatie van AuthHelper zodat extractUid/extractUidOrNull
    // correct werken zonder elke test afzonderlijk te stubben.
    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private ReadingListNotificationController readingListNotificationController;

    // --- status ---

    @Test
    void givenAllNotificationsEnabled_whenStatus_thenReturnsTrue() {
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        ReadingListEntity list = readingListWithBooks(1L, List.of(10L, 11L));
        when(readingListRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(list));
        when(bookNotificationService.isAllEnabled("uid-1", List.of(10L, 11L))).thenReturn(true);

        ResponseEntity<Boolean> response = readingListNotificationController.status(1L, oAuth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.TRUE, response.getBody());
    }

    @Test
    void givenNotAllNotificationsEnabled_whenStatus_thenReturnsFalse() {
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        ReadingListEntity list = readingListWithBooks(1L, List.of(10L, 11L));
        when(readingListRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(list));
        when(bookNotificationService.isAllEnabled("uid-1", List.of(10L, 11L))).thenReturn(false);

        ResponseEntity<Boolean> response = readingListNotificationController.status(1L, oAuth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.FALSE, response.getBody());
    }

    @Test
    void givenReadingListNotFound_whenStatus_thenThrowsNotFound() {
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(readingListRepository.findByIdWithBooks(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.status(99L, oAuth2User));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenNullPrincipal_whenStatus_thenThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.status(1L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(bookNotificationService, readingListRepository);
    }

    @Test
    void givenPrincipalWithoutUserId_whenStatus_thenThrowsUnauthorized() {
        when(oAuth2User.getAttribute("userID")).thenReturn("  ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.status(1L, oAuth2User));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(bookNotificationService, readingListRepository);
    }

    // --- enable ---

    @Test
    void givenValidPrincipalAndList_whenEnable_thenCallsEnableBulkAndReturnsOk() {
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        ReadingListEntity list = readingListWithBooks(2L, List.of(20L, 21L));
        when(readingListRepository.findByIdWithBooks(2L)).thenReturn(Optional.of(list));

        ResponseEntity<Void> response = readingListNotificationController.enable(2L, oAuth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookNotificationService, times(1)).enableBulk("uid-1", List.of(20L, 21L));
    }

    @Test
    void givenReadingListNotFound_whenEnable_thenThrowsNotFound() {
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(readingListRepository.findByIdWithBooks(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.enable(99L, oAuth2User));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(bookNotificationService, never()).enableBulk(any(), any());
    }

    @Test
    void givenNullPrincipal_whenEnable_thenThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.enable(2L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(bookNotificationService, readingListRepository);
    }

    @Test
    void givenPrincipalWithoutUserId_whenEnable_thenThrowsUnauthorized() {
        when(oAuth2User.getAttribute("userID")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.enable(2L, oAuth2User));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(bookNotificationService, readingListRepository);
    }

    // --- disable ---

    @Test
    void givenValidPrincipalAndList_whenDisable_thenCallsDisableBulkAndReturnsNoContent() {
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        ReadingListEntity list = readingListWithBooks(3L, List.of(30L));
        when(readingListRepository.findByIdWithBooks(3L)).thenReturn(Optional.of(list));

        ResponseEntity<Void> response = readingListNotificationController.disable(3L, oAuth2User);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(bookNotificationService, times(1)).disableBulk("uid-1", List.of(30L));
    }

    @Test
    void givenReadingListNotFound_whenDisable_thenThrowsNotFound() {
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(readingListRepository.findByIdWithBooks(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.disable(99L, oAuth2User));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(bookNotificationService, never()).disableBulk(any(), any());
    }

    @Test
    void givenNullPrincipal_whenDisable_thenThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListNotificationController.disable(3L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(bookNotificationService, readingListRepository);
    }

    // --- helpers ---

    private ReadingListEntity readingListWithBooks(Long listId, List<Long> bookIds) {
        ReadingListEntity list = new ReadingListEntity();
        list.setId(listId);
        LinkedHashSet<BookEntity> books = new LinkedHashSet<>();
        for (Long bookId : bookIds) {
            BookEntity book = new BookEntity();
            book.setId(bookId);
            books.add(book);
        }
        list.setBooks(books);
        return list;
    }
}
