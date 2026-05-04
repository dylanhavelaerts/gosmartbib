package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationDTO;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveClassesResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveSchoolsResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveUsersResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationTestResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.UpsertSchoolIntegrationRequest;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.schoolIntegration.SchoolIntegrationAdminService;
import edu.ap.gosmartlib.services.schoolIntegration.SchoolIntegrationService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/admin/schools/{schoolId}/integration")
@RequiredArgsConstructor
public class SchoolIntegrationAdminController {

    private final SchoolIntegrationService schoolIntegrationService;
    private final SchoolIntegrationAdminService schoolIntegrationAdminService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationDTO getIntegration(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User principal) {
        return schoolIntegrationService.getIntegration(authHelper.extractUid(principal), schoolId);
    }

    @PutMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationDTO upsertIntegration(
            @PathVariable Long schoolId,
            @RequestBody UpsertSchoolIntegrationRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        return schoolIntegrationService.upsertIntegration(authHelper.extractUid(principal), schoolId, request);
    }

    @PostMapping("/test")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationTestResponse testIntegration(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User principal) {
        return schoolIntegrationAdminService.testIntegration(authHelper.extractUid(principal), schoolId);
    }

    @GetMapping("/live/schools")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationLiveSchoolsResponse getLiveSchools(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User principal) {
        return schoolIntegrationAdminService.getLiveSchools(authHelper.extractUid(principal), schoolId);
    }

    @GetMapping("/live/users")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationLiveUsersResponse getLiveUsers(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User principal) {
        return schoolIntegrationAdminService.getLiveUsers(authHelper.extractUid(principal), schoolId);
    }

    @GetMapping("/live/classes")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationLiveClassesResponse getLiveClasses(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User principal) {
        return schoolIntegrationAdminService.getLiveClasses(authHelper.extractUid(principal), schoolId);
    }
}
