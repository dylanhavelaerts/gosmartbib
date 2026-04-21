package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO;
import edu.ap.gosmartlib.services.UserDirectoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartschoolControllerTest {

    @Mock
    private UserDirectoryService userDirectoryService;

    @Mock
    private OAuth2User principal;

    @InjectMocks
    private SmartschoolController smartschoolController;

    @Test
    void givenValidPrincipal_whenSearchSmartschoolUsers_thenDelegatesToService() {
        when(principal.getAttribute("userID")).thenReturn("admin-uid");

        List<SmartschoolUserDTO> expected = List.of(
                new SmartschoolUserDTO(
                        "piuogheziugsoqihf=",
                        "First Last",
                        "2ITSOF",
                        "AP Hogeschool",
                        "1",
                        "https://ui-avatars.com/api/?name=First+Last&background=random"));

        when(userDirectoryService.searchUsersForLoan("admin-uid", "first")).thenReturn(expected);

        ResponseEntity<List<SmartschoolUserDTO>> response = smartschoolController.searchSmartschoolUsers("first",
                principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(userDirectoryService).searchUsersForLoan("admin-uid", "first");
    }

    @Test
    void givenMissingPrincipal_whenSearchSmartschoolUsers_thenThrowsUnauthorized() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smartschoolController.searchSmartschoolUsers("first", null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void givenPrincipalWithoutUserId_whenSearchSmartschoolUsers_thenThrowsUnauthorized() {
        when(principal.getAttribute("userID")).thenReturn(" ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smartschoolController.searchSmartschoolUsers("first", principal));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}