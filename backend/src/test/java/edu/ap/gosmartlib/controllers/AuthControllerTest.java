package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.UserDTO;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2User oauth2User;

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
        verify(userRepository, never()).findBySmartschoolUid(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void givenAuthenticatedUserExists_whenMe_thenReturnsUserDto() {
        String uid = "smartschool-user-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        SchoolEntity school = new SchoolEntity();
        school.setId(10L);
        school.setName("AP Hogeschool");
        school.setDomain("aphogeschool.smartschool.be");

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setRole(UserRoles.BIBLIOTHEEKBEHEERDER);
        user.setSchool(school);
        user.setClasses(new HashSet<>());

        when(userRepository.findBySmartschoolUid(uid)).thenReturn(Optional.of(user));

        ResponseEntity<UserDTO> response = authController.me(oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals(UserRoles.BIBLIOTHEEKBEHEERDER, response.getBody().role());
        assertEquals("AP Hogeschool", response.getBody().school().name());
        verify(userRepository, times(1)).findBySmartschoolUid(uid);
    }

    @Test
    void givenAuthenticatedUserMissingInDatabase_whenMe_thenThrowsNotFound() {
        String uid = "unknown-user";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);
        when(userRepository.findBySmartschoolUid(uid)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authController.me(oauth2User));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
        verify(userRepository, times(1)).findBySmartschoolUid(uid);
    }
}

