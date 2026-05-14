package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.dto.SchoolClassDTO;
import edu.ap.gosmartlib.dto.SchoolDTO;
import edu.ap.gosmartlib.dto.UpdateUserRoleRequest;
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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
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

        // @Spy gebruikt de echte implementatie van AuthHelper zodat extractUid/extractUidOrNull
        // correct werken zonder elke test afzonderlijk te stubben.
        @Spy
        private AuthHelper authHelper = new AuthHelper();

        @InjectMocks
        private UserAdminController userAdminController;

        @Test
        void givenValidOAuthUser_whenListUsers_thenDelegatesWithExtractedUid() {
                List<AdminUserDTO> list = List.of(buildAdminUserDTO(1L, "student-uid", UserRoles.STUDENT));
                Page<AdminUserDTO> expectedPage = new PageImpl<>(list);
                Pageable pageable = PageRequest.of(0, 10);

                when(oAuth2User.getAttribute("userID")).thenReturn("admin-uid");
                when(userAdminService.listUsersForAdmin("admin-uid", null, pageable)).thenReturn(expectedPage);

                Page<AdminUserDTO> result = userAdminController.listUsers(oAuth2User, null, pageable);

                assertEquals(expectedPage, result);
                verify(oAuth2User).getAttribute("userID");
                verify(userAdminService).listUsersForAdmin("admin-uid", null, pageable);
                verifyNoMoreInteractions(userAdminService, oAuth2User);
        }

        @Test
        void givenNullOAuthUser_whenListUsers_thenThrowsUnauthorized() {
                Pageable pageable = PageRequest.of(0, 10);

                ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                () -> userAdminController.listUsers(null, null, pageable));

                assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
                assertEquals("Niet ingelogd", exception.getReason());
                verifyNoInteractions(userAdminService);
        }

        @Test
        void givenMissingUid_whenListUsers_thenThrowsUnauthorized() {
                when(oAuth2User.getAttribute("userID")).thenReturn(null);
                Pageable pageable = PageRequest.of(0, 10);

                ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                () -> userAdminController.listUsers(oAuth2User, null, pageable));

                assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
                assertEquals("Geen geldige gebruiker", exception.getReason());
                verify(oAuth2User).getAttribute("userID");
                verifyNoInteractions(userAdminService);
        }

        @Test
        void givenBlankUid_whenListUsers_thenThrowsUnauthorized() {
                when(oAuth2User.getAttribute("userID")).thenReturn("   ");
                Pageable pageable = PageRequest.of(0, 10);

                ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                () -> userAdminController.listUsers(oAuth2User, null, pageable));

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
                                new SchoolDTO(100L, "GO! School", "school.example.be"),
                                Set.of(new SchoolClassDTO(10L, "1A", "1", "2025-2026")));
        }
}