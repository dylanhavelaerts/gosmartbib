package edu.ap.gosmartlib.controllers.school;

import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationDTO;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveClassesResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveSchoolsResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveUsersResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationTestResponse;
import edu.ap.gosmartlib.dto.schoolintegration.UpsertSchoolIntegrationRequest;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.schoolintegration.SchoolIntegrationAdminService;
import edu.ap.gosmartlib.services.schoolintegration.SchoolIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("@roleGuard.isLibrarianorAdmin(authentication)")
    public SchoolIntegrationDTO getIntegration(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationService.getIntegrationForPlatformAdmin(schoolId);
        return schoolIntegrationService.getIntegration(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    @PutMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationDTO upsertIntegration(@PathVariable Long schoolId,
                                                  @RequestBody UpsertSchoolIntegrationRequest request,
                                                  Authentication authentication) {
        return schoolIntegrationService.upsertIntegrationForPlatformAdmin(schoolId, request);
    }

    @PostMapping("/test")
    @PreAuthorize("@roleGuard.isLibrarianorAdmin(authentication)")
    public SchoolIntegrationTestResponse testIntegration(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.testIntegrationForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.testIntegration(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    @GetMapping("/live/schools")
    @PreAuthorize("@roleGuard.isLibrarianorAdmin(authentication)")
    public SchoolIntegrationLiveSchoolsResponse getLiveSchools(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.getLiveSchoolsForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.getLiveSchools(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    @GetMapping("/live/users")
    @PreAuthorize("@roleGuard.isLibrarianorAdmin(authentication)")
    public SchoolIntegrationLiveUsersResponse getLiveUsers(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.getLiveUsersForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.getLiveUsers(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    @GetMapping("/live/classes")
    @PreAuthorize("@roleGuard.isLibrarianorAdmin(authentication)")
    public SchoolIntegrationLiveClassesResponse getLiveClasses(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.getLiveClassesForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.getLiveClasses(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

}
