package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.user.UserPreferenceDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.users.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final AuthHelper authHelper;

    @GetMapping
    public ResponseEntity<UserPreferenceDTO> getPreferences(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(userPreferenceService.getPreferences(uid));
    }

    @PatchMapping
    public ResponseEntity<UserPreferenceDTO> updatePreferences(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody UserPreferenceDTO dto) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(userPreferenceService.updatePreferences(uid, dto));
    }
}
