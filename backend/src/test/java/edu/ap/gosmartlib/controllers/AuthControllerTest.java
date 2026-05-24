package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.SchoolDTO;
import edu.ap.gosmartlib.dto.UserDTO;
import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.services.users.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private OAuth2User oauth2User;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    @Test
    void givenLoginRequest_whenLogin_thenRedirectsToSmartschoolAuthorization() {
        RedirectView response = authController.login();

        assertNotNull(response);
        assertEquals("/api/oauth2/authorization/smartschool", response.getUrl());
    }

    @Test
    void givenNoAuthenticatedUser_whenMe_thenReturnsUnauthorized() {
        ResponseEntity<UserDTO> response = authController.me(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(userService);
    }

    @Test
    void givenAuthenticatedUserExists_whenMe_thenReturnsUserDto() {
        String uid = "smartschool-user-1";
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);
        UserDTO expected = new UserDTO(
                1L, uid, UserRoles.BIBLIOTHEEKBEHEERDER,
                new SchoolDTO(10L, "AP Hogeschool", "aphogeschool.smartschool.be"),
                Set.of());
        when(userService.getCurrentUser(uid)).thenReturn(expected);

        ResponseEntity<UserDTO> response = authController.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().id());
        assertEquals(UserRoles.BIBLIOTHEEKBEHEERDER, response.getBody().role());
        verify(userService, times(1)).getCurrentUser(uid);
    }


    @Test
    void givenAuthenticatedUserMissingInDatabase_whenMe_thenThrowsRuntimeExceptionFromService() {
        String uid = "unknown-user";
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);
        when(userService.getCurrentUser(uid)).thenThrow(new RuntimeException("Gebruiker niet gevonden"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authController.me(authentication));

        assertEquals("Gebruiker niet gevonden", ex.getMessage());
        verify(userService, times(1)).getCurrentUser(uid);
    }

    @Test
    void givenAdminPrincipal_whenMe_thenReturnsAdminUserDtoWithoutSchool() {
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setId(1L);
        adminEntity.setUsername("admin");
        AdminPrincipal adminPrincipal = new AdminPrincipal(adminEntity);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(adminPrincipal);

        ResponseEntity<UserDTO> response = authController.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(UserRoles.ADMIN, response.getBody().role());
        assertNull(response.getBody().school());
        assertTrue(response.getBody().classes().isEmpty());
        verifyNoInteractions(userService);
    }

    @Test
    void givenUnauthenticatedAuthentication_whenMe_thenReturnsUnauthorized() {
        when(authentication.isAuthenticated()).thenReturn(false);

        ResponseEntity<UserDTO> response = authController.me(authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(userService);
    }
}

