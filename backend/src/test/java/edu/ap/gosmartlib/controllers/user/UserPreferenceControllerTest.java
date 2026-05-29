package edu.ap.gosmartlib.controllers.user;

import edu.ap.gosmartlib.dto.user.UserPreferenceDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.users.UserPreferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceControllerTest {

    @Mock private UserPreferenceService userPreferenceService;
    @Mock private AuthHelper authHelper;
    @Mock private OAuth2User principal;

    @InjectMocks
    private UserPreferenceController userPreferenceController;

    private static final String UID = "user-uid";

    @Test
    void givenValidPrincipal_whenGetPreferences_thenReturnsOkWithPreferences() {
        UserPreferenceDTO prefs = mock(UserPreferenceDTO.class);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(userPreferenceService.getPreferences(UID)).thenReturn(prefs);

        ResponseEntity<UserPreferenceDTO> response = userPreferenceController.getPreferences(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(prefs, response.getBody());
        verify(userPreferenceService).getPreferences(UID);
    }

    @Test
    void givenValidPrincipal_whenUpdatePreferences_thenReturnsOkWithUpdated() {
        UserPreferenceDTO dto = mock(UserPreferenceDTO.class);
        UserPreferenceDTO updated = mock(UserPreferenceDTO.class);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(userPreferenceService.updatePreferences(UID, dto)).thenReturn(updated);

        ResponseEntity<UserPreferenceDTO> response = userPreferenceController.updatePreferences(principal, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
        verify(userPreferenceService).updatePreferences(UID, dto);
    }
}
