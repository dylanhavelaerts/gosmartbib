package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.dto.SchoolClassDTO;
import edu.ap.gosmartlib.dto.SchoolDTO;
import edu.ap.gosmartlib.dto.UpdateUserRoleRequest;
import edu.ap.gosmartlib.services.UserAdminService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

    @Mock
    private UserAdminService userAdminService;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private UserAdminController userAdminController;

    @Test
    void givenValidOAuthUser_whenListUsers_thenDelegatesWithExtractedUid() {
        Pageable pageable = Pageable.unpaged();
        String name = null;

        Page<AdminUserDTO> expected = new PageImpl<>(
                List.of(buildAdminUserDTO(1L, "student-uid", UserRoles.STUDENT)));

        when(oAuth2User.getAttribute("userID")).thenReturn("admin-uid");
        when(userAdminService.listUsersForAdmin("admin-uid", name, pageable)).thenReturn(expected);

        Page<AdminUserDTO> result = userAdminController.listUsers(oAuth2User, name, pageable);

        assertEquals(expected, result);
        verify(oAuth2User).getAttribute("userID");
        verify(userAdminService).listUsersForAdmin("admin-uid", name, pageable);
        verifyNoMoreInteractions(userAdminService, oAuth2User);
    }

    @Test
    void givenNullOAuthUser_whenListUsers_thenThrowsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.listUsers(null, null, Pageable.unpaged()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Niet ingelogd", exception.getReason());
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenMissingUid_whenListUsers_thenThrowsUnauthorized() {
        when(oAuth2User.getAttribute("userID")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.listUsers(oAuth2User, null, Pageable.unpaged()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenBlankUid_whenListUsers_thenThrowsUnauthorized() {
        when(oAuth2User.getAttribute("userID")).thenReturn("   ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.listUsers(oAuth2User, null, Pageable.unpaged()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenValidOAuthUserAndRequest_whenUpdateRole_thenDelegatesToService() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRoles.TEACHER);
        AdminUserDTO expected = buildAdminUserDTO(2L, "student-uid", UserRoles.TEACHER);

        when(oAuth2User.getAttribute("userID")).thenReturn("admin-uid");
        when(userAdminService.updateUserRole("admin-uid", 2L, UserRoles.TEACHER)).thenReturn(expected);

        AdminUserDTO result = userAdminController.updateRole(2L, request, oAuth2User);

        assertEquals(expected, result);
        verify(oAuth2User).getAttribute("userID");
        verify(userAdminService).updateUserRole("admin-uid", 2L, UserRoles.TEACHER);
        verifyNoMoreInteractions(userAdminService, oAuth2User);
    }

    @Test
    void givenNullOAuthUser_whenUpdateRole_thenThrowsUnauthorized() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRoles.TEACHER);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.updateRole(2L, request, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Niet ingelogd", exception.getReason());
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenBlankUid_whenUpdateRole_thenThrowsUnauthorized() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRoles.TEACHER);
        when(oAuth2User.getAttribute("userID")).thenReturn(" ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.updateRole(2L, request, oAuth2User));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(userAdminService);
    }

    private AdminUserDTO buildAdminUserDTO(Long id, String uid, UserRoles role) {
        return new AdminUserDTO(
                id,
                uid,
                role,
                true,
                new SchoolDTO(100L, "GO! School", "school.example.be"),
                Set.of(new SchoolClassDTO(10L, "1A", "1", "2025-2026")));
    }
}