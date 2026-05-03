package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
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

    @GetMapping("/{id}")
    public ResponseEntity<ReadingListDetailDTO> getListDetail(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        ReadingListDetailDTO detail = readingListService.getListDetail(id, authHelper.extractUid(principal));
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/class")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
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

    @DeleteMapping("/personal/{id}")
    public ResponseEntity<Void> deletePersonalList(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        readingListService.deletePersonalList(id, authHelper.extractUid(principal));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/class/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    public ResponseEntity<Void> deleteClassList(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        readingListService.deleteClassList(id, authHelper.extractUid(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/class/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    public ResponseEntity<Long> updateClassList(@PathVariable Long id, @RequestBody CreateReadingListDTO dto, @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity updated = readingListService.updateClassList(id, dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(updated.getId());
    }
}
