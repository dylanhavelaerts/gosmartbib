package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.dto.SchoolClassDTO;
import edu.ap.gosmartlib.dto.SchoolDTO;
import edu.ap.gosmartlib.dto.UpdateUserRoleRequest;
import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.users.UserAdminService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

    @Mock private UserAdminService userAdminService;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;

    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private UserAdminController userAdminController;

    @Test
    void givenValidOAuthUser_whenListUsers_thenDelegatesWithExtractedUid() {
        List<AdminUserDTO> list = List.of(buildAdminUserDTO(1L, "student-uid", UserRoles.STUDENT));
        Page<AdminUserDTO> expectedPage = new PageImpl<>(list);
        Pageable pageable = PageRequest.of(0, 10);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("bibbeheerder-uid");
        when(userAdminService.listUsersForLibrarian("bibbeheerder-uid", null, null, pageable)).thenReturn(expectedPage);

        Page<AdminUserDTO> result = userAdminController.listUsers(authentication, null, null, pageable);

        assertEquals(expectedPage, result);
        verify(authentication, times(2)).getPrincipal();
        verify(oAuth2User).getAttribute("userID");
        verify(userAdminService).listUsersForLibrarian("bibbeheerder-uid", null, null, pageable);
        verifyNoMoreInteractions(userAdminService, authentication, oAuth2User);
    }

    @Test
    void givenNullPrincipal_whenListUsers_thenThrowsUnauthorized() {
        Pageable pageable = PageRequest.of(0, 10);
        when(authentication.getPrincipal()).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.listUsers(authentication, null, null, pageable));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Niet ingelogd", exception.getReason());
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenMissingUid_whenListUsers_thenThrowsUnauthorized() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn(null);
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.listUsers(authentication, null, null, pageable));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenBlankUid_whenListUsers_thenThrowsUnauthorized() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("   ");
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.listUsers(authentication, null, null, pageable));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenValidOAuthUserAndRequest_whenUpdateRole_thenDelegatesToService() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRoles.TEACHER);
        AdminUserDTO expected = buildAdminUserDTO(2L, "student-uid", UserRoles.TEACHER);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("bibbeheerder-uid");
        when(userAdminService.updateUserRoleForLibrarian("bibbeheerder-uid", null, 2L, UserRoles.TEACHER)).thenReturn(expected);

        AdminUserDTO result = userAdminController.updateRole(2L, null, request, authentication);

        assertEquals(expected, result);
        verify(authentication, times(2)).getPrincipal();
        verify(oAuth2User).getAttribute("userID");
        verify(userAdminService).updateUserRoleForLibrarian("bibbeheerder-uid", null, 2L, UserRoles.TEACHER);
        verifyNoMoreInteractions(userAdminService, authentication, oAuth2User);
    }

    @Test
    void givenNullPrincipal_whenUpdateRole_thenThrowsUnauthorized() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRoles.TEACHER);
        when(authentication.getPrincipal()).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.updateRole(2L, null, request, authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Niet ingelogd", exception.getReason());
        verifyNoInteractions(userAdminService);
    }

    @Test
    void givenBlankUid_whenUpdateRole_thenThrowsUnauthorized() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRoles.TEACHER);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn(" ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminController.updateRole(2L, null, request, authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(userAdminService);
    }
    @Test
    void givenAdminPrincipal_whenListUsers_thenDelegatesForPlatformAdmin() {
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setId(1L);
        AdminPrincipal adminPrincipal = new AdminPrincipal(adminEntity);

        List<AdminUserDTO> list = List.of(buildAdminUserDTO(1L, "student-uid", UserRoles.STUDENT));
        Page<AdminUserDTO> expectedPage = new PageImpl<>(list);
        Pageable pageable = PageRequest.of(0, 10);

        when(authentication.getPrincipal()).thenReturn(adminPrincipal);
        when(userAdminService.listUsersForPlatformAdmin(100L, null, pageable)).thenReturn(expectedPage);

        Page<AdminUserDTO> result = userAdminController.listUsers(authentication, 100L, null, pageable);

        assertEquals(expectedPage, result);
        verify(authentication).getPrincipal();
        verify(userAdminService).listUsersForPlatformAdmin(100L, null, pageable);
        verifyNoMoreInteractions(userAdminService, authentication);
    }

    @Test
    void givenAdminPrincipal_whenUpdateRole_thenDelegatesForPlatformAdmin() {
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setId(1L);
        AdminPrincipal adminPrincipal = new AdminPrincipal(adminEntity);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRoles.TEACHER);
        AdminUserDTO expected = buildAdminUserDTO(2L, "student-uid", UserRoles.TEACHER);

        when(authentication.getPrincipal()).thenReturn(adminPrincipal);
        when(userAdminService.updateUserRoleForPlatformAdmin(100L, 2L, UserRoles.TEACHER)).thenReturn(expected);

        AdminUserDTO result = userAdminController.updateRole(2L, 100L, request, authentication);

        assertEquals(expected, result);
        verify(authentication).getPrincipal();
        verify(userAdminService).updateUserRoleForPlatformAdmin(100L, 2L, UserRoles.TEACHER);
        verifyNoMoreInteractions(userAdminService, authentication);
    }


    private AdminUserDTO buildAdminUserDTO(Long id, String uid, UserRoles role) {
        return new AdminUserDTO(
                id, uid, role,
                new SchoolDTO(100L, "GO! School", "school.example.be"),
                Set.of(new SchoolClassDTO(10L, "1A", "1", "2025-2026")));
    }
}
