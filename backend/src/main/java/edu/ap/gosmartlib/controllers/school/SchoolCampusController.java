package edu.ap.gosmartlib.controllers.school;

import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.CreateSchoolCampusRequest;
import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.SchoolCampusDTO;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.schoolIntegration.schoolCampus.SchoolCampusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/schools/{schoolId}/campuses")
@RequiredArgsConstructor
public class SchoolCampusController {

    private final SchoolCampusService schoolCampusService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication) or @roleGuard.isBibbeheerder(authentication)")
    public List<SchoolCampusDTO> getCampuses(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolCampusService.getCampusesForPlatformAdmin(schoolId);
        return schoolCampusService.getCampusesForBibbeheerder(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@roleGuard.isAdmin(authentication) or @roleGuard.isBibbeheerder(authentication)")
    public SchoolCampusDTO createCampus(@PathVariable Long schoolId,
                                        @Valid @RequestBody CreateSchoolCampusRequest request,
                                        Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolCampusService.createCampusForPlatformAdmin(schoolId, request);
        return schoolCampusService.createCampusForBibbeheerder(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId, request);
    }

    @DeleteMapping("/{campusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@roleGuard.isAdmin(authentication) or @roleGuard.isBibbeheerder(authentication)")
    public void deleteCampus(@PathVariable Long schoolId, @PathVariable Long campusId,
                             Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal) {
            schoolCampusService.deleteCampusForPlatformAdmin(schoolId, campusId);
            return;
        }
        schoolCampusService.deleteCampusForBibbeheerder(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId, campusId);
    }

}
