package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.dto.user.UserPreferenceDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserPreferenceService userPreferenceService;

    private static final String UID = "student-uid";

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setSmartschoolUid(UID);
        user.setAnonymousLeaderboard(true);
    }

    @Test
    void givenExistingUser_whenGetPreferences_thenReturnsCurrentPreferences() {
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));

        UserPreferenceDTO result = userPreferenceService.getPreferences(UID);

        assertTrue(result.anonymousLeaderboard());
    }

    @Test
    void givenUserWithAnonymousLeaderboardFalse_whenGetPreferences_thenReturnsFalse() {
        user.setAnonymousLeaderboard(false);
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));

        UserPreferenceDTO result = userPreferenceService.getPreferences(UID);

        assertFalse(result.anonymousLeaderboard());
    }

    @Test
    void givenUserNotFound_whenGetPreferences_thenThrows404() {
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userPreferenceService.getPreferences(UID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenExistingUser_whenUpdatePreferences_thenSavesAndReturnsUpdatedValue() {
        UserPreferenceDTO dto = new UserPreferenceDTO(false);
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));

        UserPreferenceDTO result = userPreferenceService.updatePreferences(UID, dto);

        assertFalse(result.anonymousLeaderboard());
        verify(userRepository).save(user);
        assertFalse(user.isAnonymousLeaderboard());
    }

    @Test
    void givenUserWithAnonymousDisabled_whenUpdatePreferencesToEnabled_thenSavesAndReturnsTrue() {
        user.setAnonymousLeaderboard(false);
        UserPreferenceDTO dto = new UserPreferenceDTO(true);
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));

        UserPreferenceDTO result = userPreferenceService.updatePreferences(UID, dto);

        assertTrue(result.anonymousLeaderboard());
        verify(userRepository).save(user);
    }

    @Test
    void givenUserNotFound_whenUpdatePreferences_thenThrows404() {
        UserPreferenceDTO dto = new UserPreferenceDTO(false);
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userPreferenceService.updatePreferences(UID, dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }
}
