package edu.ap.gosmartlib.controllers.school;

import edu.ap.gosmartlib.dto.school.SchoolLibrarySettingsDTO;
import edu.ap.gosmartlib.services.school.SchoolLibrarySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/schools/{schoolId}/library-settings")
@RequiredArgsConstructor
public class SchoolLibrarySettingsController {

    private final SchoolLibrarySettingsService librarySettingsService;

    @GetMapping
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<SchoolLibrarySettingsDTO> getSettings(@PathVariable Long schoolId) {
        return ResponseEntity.ok(librarySettingsService.getSettings(schoolId));
    }

    @PutMapping
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<SchoolLibrarySettingsDTO> saveSettings(
            @PathVariable Long schoolId,
            @RequestBody SchoolLibrarySettingsDTO request) {
        return ResponseEntity.ok(librarySettingsService.saveSettings(schoolId, request));
    }
}
