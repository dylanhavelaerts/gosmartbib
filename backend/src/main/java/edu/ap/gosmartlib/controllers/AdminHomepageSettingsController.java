package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.school.HomepageSettingsDTO;
import edu.ap.gosmartlib.services.school.HomepageSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/schools/{schoolId}/homepage-settings")
@RequiredArgsConstructor
public class AdminHomepageSettingsController {

    private final HomepageSettingsService homepageSettingsService;

    @GetMapping
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<HomepageSettingsDTO> getSettings(@PathVariable Long schoolId) {
        return ResponseEntity.ok(homepageSettingsService.getSettings(schoolId));
    }

    @PutMapping
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<HomepageSettingsDTO> saveSettings(@PathVariable Long schoolId, @RequestBody HomepageSettingsDTO request) {
        return ResponseEntity.ok(homepageSettingsService.saveSettings(schoolId, request));
    }
}