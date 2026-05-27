package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.PublicReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.readinglist.UpdateReadingListVisibilityDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListVisibilityDTO;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.ReadingListService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reading-lists")
public class ReadingListController {

    private final ReadingListService readingListService;
    private final AuthHelper authHelper;

    public ReadingListController(ReadingListService readingListService, AuthHelper authHelper) {
        this.readingListService = readingListService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public ResponseEntity<List<ReadingListOverviewDTO>> getVisibleLists(@AuthenticationPrincipal OAuth2User principal) {
        List<ReadingListOverviewDTO> lists = readingListService.getVisibleLists(authHelper.extractUid(principal));
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/assignment-targets")
    @PreAuthorize("hasAnyRole('TEACHER','LIBRARIAN','ADMIN')")
    public ResponseEntity<ReadingListAssignmentTargetsDTO> getAssignmentTargets(@AuthenticationPrincipal OAuth2User principal) {
        ReadingListAssignmentTargetsDTO targets = readingListService.getAssignmentTargets(authHelper.extractUid(principal));
        return ResponseEntity.ok(targets);
    }

    @GetMapping("/assignment-targets/students")
    @PreAuthorize("hasAnyRole('TEACHER','LIBRARIAN','ADMIN')")
    public ResponseEntity<?> searchAssignmentStudents(
            @RequestParam(defaultValue = "") String query,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(readingListService.searchAssignmentStudents(authHelper.extractUid(principal), query));
    }

    @GetMapping("/assignment-targets/classes")
    @PreAuthorize("hasAnyRole('TEACHER','LIBRARIAN','ADMIN')")
    public ResponseEntity<?> searchAssignmentClasses(
            @RequestParam(defaultValue = "") String query,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(readingListService.searchAssignmentClasses(authHelper.extractUid(principal), query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReadingListDetailDTO> getListDetail(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        ReadingListDetailDTO detail = readingListService.getListDetail(id, authHelper.extractUid(principal));
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/shared/{publicUid}")
    public ResponseEntity<PublicReadingListDetailDTO> getPublicListDetail(@PathVariable String publicUid) {
        PublicReadingListDetailDTO detail = readingListService.getPublicListDetail(publicUid);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/class")
    @PreAuthorize("hasAnyRole('TEACHER','LIBRARIAN','ADMIN')")
    public ResponseEntity<Long> createClassList(@RequestBody CreateReadingListDTO dto, @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity created = readingListService.createClassList(dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(created.getId());
    }

    @PostMapping("/personal")
    public ResponseEntity<Long> createPersonalList(@RequestBody CreateReadingListDTO dto, @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity created = readingListService.createPersonalList(dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(created.getId());
    }

    @PutMapping("/personal/{id}")
    public ResponseEntity<Long> updatePersonalList(
            @PathVariable Long id,
            @RequestBody CreateReadingListDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity updated = readingListService.updatePersonalList(id, dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(updated.getId());
    }

    @PatchMapping("/personal/{id}/visibility")
    public ResponseEntity<ReadingListVisibilityDTO> updatePersonalListVisibility(
            @PathVariable Long id,
            @RequestBody UpdateReadingListVisibilityDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListVisibilityDTO updated = readingListService.updatePersonalListVisibility(
                id,
                dto.publicVisible(),
                authHelper.extractUid(principal));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/personal/{id}")
    public ResponseEntity<Void> deletePersonalList(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        readingListService.deletePersonalList(id, authHelper.extractUid(principal));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/class/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','LIBRARIAN','ADMIN')")
    public ResponseEntity<Void> deleteClassList(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        readingListService.deleteClassList(id, authHelper.extractUid(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/class/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','LIBRARIAN','ADMIN')")
    public ResponseEntity<Long> updateClassList(@PathVariable Long id, @RequestBody CreateReadingListDTO dto, @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity updated = readingListService.updateClassList(id, dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(updated.getId());
    }
}
