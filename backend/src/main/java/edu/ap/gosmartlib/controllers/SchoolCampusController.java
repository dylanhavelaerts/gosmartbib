package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.CreateSchoolCampusRequest;
import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.SchoolCampusDTO;
import edu.ap.gosmartlib.services.schoolIntegration.schoolCampus.SchoolCampusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/schools/{schoolId}/campuses")
@RequiredArgsConstructor
public class SchoolCampusController {

    private final SchoolCampusService schoolCampusService;

    @GetMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication) or @roleGuard.isBibbeheerder(authentication)")
    public List<SchoolCampusDTO> getCampuses(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolCampusService.getCampusesForAdminSchool(extractUid(oAuth2User), schoolId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@roleGuard.isAdmin(authentication) or @roleGuard.isBibbeheerder(authentication)")
    public SchoolCampusDTO createCampus(
            @PathVariable Long schoolId,
            @Valid @RequestBody CreateSchoolCampusRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return schoolCampusService.createCampusForAdminSchool(extractUid(oAuth2User), schoolId, request);
    }

    @DeleteMapping("/{campusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@roleGuard.isAdmin(authentication) or @roleGuard.isBibbeheerder(authentication)")
    public void deleteCampus(
            @PathVariable Long schoolId,
            @PathVariable Long campusId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        schoolCampusService.deleteCampusForAdminSchool(extractUid(oAuth2User), schoolId, campusId);
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