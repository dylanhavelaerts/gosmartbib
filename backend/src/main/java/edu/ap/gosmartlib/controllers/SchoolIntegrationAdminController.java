package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationDTO;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveClassesResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveSchoolsResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveUsersResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationTestResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.UpsertSchoolIntegrationRequest;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.SmartschoolMessageService;
import edu.ap.gosmartlib.services.schoolIntegration.SchoolIntegrationAdminService;
import edu.ap.gosmartlib.services.schoolIntegration.SchoolIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/schools/{schoolId}/integration")
@RequiredArgsConstructor
public class SchoolIntegrationAdminController {

    private final SchoolIntegrationService schoolIntegrationService;
    private final SchoolIntegrationAdminService schoolIntegrationAdminService;
    private final UserRepository userRepository;
    private final SmartschoolMessageService smartschoolMessageService;

    @GetMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationDTO getIntegration(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolIntegrationService.getIntegration(extractUid(oAuth2User), schoolId);
    }

    @PutMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationDTO upsertIntegration(
            @PathVariable Long schoolId,
            @RequestBody UpsertSchoolIntegrationRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolIntegrationService.upsertIntegration(extractUid(oAuth2User), schoolId, request);
    }

    @PostMapping("/test")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationTestResponse testIntegration(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolIntegrationAdminService.testIntegration(extractUid(oAuth2User), schoolId);
    }

    @GetMapping("/live/schools")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationLiveSchoolsResponse getLiveSchools(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolIntegrationAdminService.getLiveSchools(extractUid(oAuth2User), schoolId);
    }

    @GetMapping("/live/users")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationLiveUsersResponse getLiveUsers(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolIntegrationAdminService.getLiveUsers(extractUid(oAuth2User), schoolId);
    }

    @GetMapping("/live/classes")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationLiveClassesResponse getLiveClasses(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolIntegrationAdminService.getLiveClasses(extractUid(oAuth2User), schoolId);
    }
    //Hardcode om te testen -> volledige methode gaat weg
    @GetMapping("/test-message")
    public ResponseEntity<String> testMessage() {
        smartschoolMessageService.sendTestMessage("sof2.benjamin.deloore");
        return ResponseEntity.ok("Bericht verstuurd");
    }

    private String extractUid(OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd");
        }

        String uid = oAuth2User.getAttribute("userID");
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Geen geldige gebruiker");
        }

        return uid;
    }
}