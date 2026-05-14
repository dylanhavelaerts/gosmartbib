package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.sync.SyncSummaryDTO;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.oneRoster.OneRosterSyncService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock private OneRosterSyncService syncService;
    @Mock private UserRepository userRepository;
    @Mock private OAuth2User oauth2User;

    @InjectMocks
    private SyncController syncController;

    @Test
    void givenAdminUser_whenSyncAll_thenDelegatesToService() {
        UserEntity admin = adminUser();
        SyncSummaryDTO expected = new SyncSummaryDTO(3, 1, List.of());

        when(oauth2User.getAttribute("userID")).thenReturn("admin-uid");
        when(userRepository.findBySmartschoolUid("admin-uid")).thenReturn(Optional.of(admin));
        when(syncService.syncAll()).thenReturn(expected);

        SyncSummaryDTO result = syncController.syncAll(oauth2User);

        assertEquals(expected, result);
        verify(syncService).syncAll();
    }

    @Test
    void givenNonAdminUser_whenSyncAll_thenThrowsForbidden() {
        UserEntity teacher = user(UserRoles.TEACHER);

        when(oauth2User.getAttribute("userID")).thenReturn("teacher-uid");
        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> syncController.syncAll(oauth2User));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Geen toegang", exception.getReason());
        verifyNoInteractions(syncService);
    }

    @Test
    void givenUnknownUser_whenSyncAll_thenThrowsNotFound() {
        when(oauth2User.getAttribute("userID")).thenReturn("unknown-uid");
        when(userRepository.findBySmartschoolUid("unknown-uid")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> syncController.syncAll(oauth2User));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Gebruiker niet gevonden", exception.getReason());
        verifyNoInteractions(syncService);
    }

    private UserEntity adminUser() {
        return user(UserRoles.ADMIN);
    }

    private UserEntity user(UserRoles role) {
        SchoolEntity school = new SchoolEntity();
        school.setId(1L);
        school.setName("GO! School");
        school.setDomain("go.school.be");

        UserEntity user = new UserEntity();
        user.setSmartschoolUid(role.name().toLowerCase() + "-uid");
        user.setRole(role);
        user.setSchool(school);
        return user;
    }
}
