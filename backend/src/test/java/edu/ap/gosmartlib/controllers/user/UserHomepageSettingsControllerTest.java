package edu.ap.gosmartlib.controllers.user;

import edu.ap.gosmartlib.dto.SchoolDTO;
import edu.ap.gosmartlib.dto.school.HomepageSettingsDTO;
import edu.ap.gosmartlib.dto.user.UserDTO;
import edu.ap.gosmartlib.services.school.HomepageSettingsService;
import edu.ap.gosmartlib.services.users.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserHomepageSettingsControllerTest {

    @Mock private HomepageSettingsService homepageSettingsService;
    @Mock private UserService userService;
    @Mock private OAuth2User oauth2User;

    @InjectMocks
    private UserHomepageSettingsController userHomepageSettingsController;

    private static final String UID = "user-uid";

    @Test
    void givenValidPrincipal_whenGetMySettings_thenReturnsOkWithSettings() {
        SchoolDTO school = new SchoolDTO(10L, "My School", "school.be");
        UserDTO user = new UserDTO(1L, UID, UserRoles.STUDENT, school, Set.of());
        HomepageSettingsDTO settings = mock(HomepageSettingsDTO.class);
        when(oauth2User.getAttribute("userID")).thenReturn(UID);
        when(userService.getCurrentUser(UID)).thenReturn(user);
        when(homepageSettingsService.getSettings(10L)).thenReturn(settings);

        ResponseEntity<HomepageSettingsDTO> response = userHomepageSettingsController.getMySettings(oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(settings, response.getBody());
        verify(homepageSettingsService).getSettings(10L);
    }

    @Test
    void givenNullPrincipal_whenGetMySettings_thenReturnsUnauthorized() {
        ResponseEntity<HomepageSettingsDTO> response = userHomepageSettingsController.getMySettings(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(userService);
        verifyNoInteractions(homepageSettingsService);
    }
}
