package edu.ap.gosmartlib.controllers.statistics;

import edu.ap.gosmartlib.dto.statistics.PersonalReadingStatDTO;
import edu.ap.gosmartlib.dto.statistics.ReaderProfileDTO;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.statistics.UserStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user-stats")
@RequiredArgsConstructor
public class UserStatController {

    private final UserStatsService userStatsService;
    private final AuthHelper authHelper;
    private final UserRepository userRepository;

    @GetMapping("/personal")
    public ResponseEntity<PersonalReadingStatDTO> getPersonalStats(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);

        return ResponseEntity.ok(userStatsService.getPersonalReadingStats(uid));
    }

    @GetMapping("/reader-profile")
    public ResponseEntity<ReaderProfileDTO> getReaderProfile(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);

        return ResponseEntity.ok(userStatsService.getReaderProfile(uid));
    }

    /**
     * Endpoint om de verdeling van lezersprofielen in een school op te halen. De response is een map
     * van profielnaam naar aantal gebruikers met dat profiel. 
     * Bijvoorbeeld: { "Avonturier": 10, "Pionier": 5, "Sprinter": 3, "Titan": 2 }
     */
    @GetMapping("/profile-distribution")
    public ResponseEntity<Map<String, Integer>> getProfileDistribution(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        Long schoolId = userRepository.findBySmartschoolUid(uid)
                .map(u -> u.getSchool().getId())
                .orElseThrow();

        return ResponseEntity.ok(userStatsService.getProfileDistribution(schoolId));
    }
}
