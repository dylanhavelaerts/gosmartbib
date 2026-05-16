package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.messages.BookNotificationService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookNotificationControllerTest {

    @Mock
    private BookNotificationService bookNotificationService;

    @Mock
    private OAuth2User principal;

    // @Spy gebruikt de echte implementatie van AuthHelper zodat extractUid/extractUidOrNull
    // correct werken zonder elke test afzonderlijk te stubben.
    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private BookNotificationController bookNotificationController;

    // --- enable ---

    @Test
    void givenValidPrincipal_whenEnable_thenDelegatesToServiceAndReturnsOk() {
        when(principal.getAttribute("userID")).thenReturn("uid-1");

        ResponseEntity<Void> response = bookNotificationController.enable(10L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookNotificationService, times(1)).enable("uid-1", 10L);
    }

    @Test
    void givenNullPrincipal_whenEnable_thenThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookNotificationController.enable(10L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void givenPrincipalWithoutUserId_whenEnable_thenThrowsUnauthorized() {
        when(principal.getAttribute("userID")).thenReturn(" ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookNotificationController.enable(10L, principal));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(bookNotificationService);
    }

    // --- disable ---

    @Test
    void givenValidPrincipal_whenDisable_thenDelegatesToServiceAndReturnsNoContent() {
        when(principal.getAttribute("userID")).thenReturn("uid-1");

        ResponseEntity<Void> response = bookNotificationController.disable(10L, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(bookNotificationService, times(1)).disable("uid-1", 10L);
    }

    @Test
    void givenNullPrincipal_whenDisable_thenThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookNotificationController.disable(10L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // --- status ---

    @Test
    void givenNotificationEnabled_whenGetStatus_thenReturnsTrue() {
        when(principal.getAttribute("userID")).thenReturn("uid-1");
        when(bookNotificationService.isEnabled("uid-1", 10L)).thenReturn(true);

        ResponseEntity<Boolean> response = bookNotificationController.status(10L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.TRUE, response.getBody());
    }

    @Test
    void givenNotificationDisabled_whenGetStatus_thenReturnsFalse() {
        when(principal.getAttribute("userID")).thenReturn("uid-1");
        when(bookNotificationService.isEnabled("uid-1", 10L)).thenReturn(false);

        ResponseEntity<Boolean> response = bookNotificationController.status(10L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.FALSE, response.getBody());
    }

    @Test
    void givenNullPrincipal_whenGetStatus_thenThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookNotificationController.status(10L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
