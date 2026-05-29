package edu.ap.gosmartlib.controllers.statistics;

import edu.ap.gosmartlib.dto.statistics.AchievementDTO;
import edu.ap.gosmartlib.dto.statistics.PersonalReadingStatDTO;
import edu.ap.gosmartlib.dto.statistics.ReaderProfileDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.statistics.AchievementService;
import edu.ap.gosmartlib.services.statistics.UserStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStatControllerTest {

    @Mock private UserStatsService userStatsService;
    @Mock private AchievementService achievementService;
    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private OAuth2User principal;

    @InjectMocks
    private UserStatController userStatController;

    private static final String UID = "student-uid";

    @Test
    void givenValidPrincipal_whenGetPersonalStats_thenReturnsOk() {
        PersonalReadingStatDTO stats = mock(PersonalReadingStatDTO.class);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(userStatsService.getPersonalReadingStats(UID)).thenReturn(stats);

        ResponseEntity<PersonalReadingStatDTO> response = userStatController.getPersonalStats(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(stats, response.getBody());
        verify(userStatsService).getPersonalReadingStats(UID);
    }

    @Test
    void givenValidPrincipal_whenGetReaderProfile_thenReturnsOk() {
        ReaderProfileDTO profile = mock(ReaderProfileDTO.class);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(userStatsService.getReaderProfile(UID)).thenReturn(profile);

        ResponseEntity<ReaderProfileDTO> response = userStatController.getReaderProfile(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(profile, response.getBody());
        verify(userStatsService).getReaderProfile(UID);
    }

    @Test
    void givenValidPrincipal_whenGetAchievements_thenReturnsOk() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(achievementService.getAchievements(UID)).thenReturn(List.of());

        ResponseEntity<List<AchievementDTO>> response = userStatController.getAchievements(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(achievementService).getAchievements(UID);
    }

    @Test
    void givenValidPrincipal_whenGetProfileDistribution_thenLooksUpSchoolAndDelegates() {
        SchoolEntity school = mock(SchoolEntity.class);
        UserEntity user = mock(UserEntity.class);
        when(school.getId()).thenReturn(42L);
        when(user.getSchool()).thenReturn(school);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));
        when(userStatsService.getProfileDistribution(42L)).thenReturn(Map.of("Avonturier", 5));

        ResponseEntity<Map<String, Integer>> response = userStatController.getProfileDistribution(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("Avonturier", 5), response.getBody());
        verify(userStatsService).getProfileDistribution(42L);
    }
}
