package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.services.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserDirectoryControllerTest {

    @Mock
    private UserDirectoryService userDirectoryService;
    @Mock
    private UserService userService;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private UserDirectoryController userDirectoryController;

    @Test
    void givenAuthenticatedRequest_whenLogout_thenDelegatesLogoutToServiceAndReturns200() {
        ResponseEntity<Void> result = userDirectoryController.logout(response);

        verify(userService, times(1)).logUserOut(response);
        verifyNoInteractions(userDirectoryService);
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
