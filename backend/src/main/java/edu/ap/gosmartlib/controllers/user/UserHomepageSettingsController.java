package edu.ap.gosmartlib.controllers.user;

import edu.ap.gosmartlib.dto.school.HomepageSettingsDTO;
import edu.ap.gosmartlib.dto.user.UserDTO;
import edu.ap.gosmartlib.services.school.HomepageSettingsService;
import edu.ap.gosmartlib.services.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schools/my-homepage-settings")
@RequiredArgsConstructor
public class UserHomepageSettingsController {

    private final HomepageSettingsService homepageSettingsService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<HomepageSettingsDTO> getMySettings(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) return ResponseEntity.status(401).build();
        
        String uid = oauth2User.getAttribute("userID");
        UserDTO user = userService.getCurrentUser(uid);
        
        return ResponseEntity.ok(homepageSettingsService.getSettings(user.school().id()));
    }
}