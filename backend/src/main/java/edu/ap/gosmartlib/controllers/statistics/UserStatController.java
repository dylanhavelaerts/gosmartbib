package edu.ap.gosmartlib.controllers.statistics;

import edu.ap.gosmartlib.dto.statistics.PersonalReadingStatDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.statistics.UserStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-stats")
@RequiredArgsConstructor
public class UserStatController {

    private final UserStatsService userStatsService;
    private final AuthHelper authHelper;

    @GetMapping("/personal")
    public ResponseEntity<PersonalReadingStatDTO> getPersonalStats(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);

        return ResponseEntity.ok(userStatsService.getPersonalReadingStats(uid));
    }
}
