package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.PublicReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.readinglist.UpdateReadingListVisibilityDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListVisibilityDTO;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.services.ReadingListService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reading-lists")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService readingListService;

    @GetMapping
    public ResponseEntity<?> getVisibleLists(Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            List<ReadingListOverviewDTO> lists = readingListService.getVisibleLists(uid);
            return ResponseEntity.ok(lists);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not load reading lists: " + e.getMessage());
        }
    }

    @GetMapping("/assignment-targets")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    public ResponseEntity<?> getAssignmentTargets(Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            ReadingListAssignmentTargetsDTO targets = readingListService.getAssignmentTargets(uid);
            return ResponseEntity.ok(targets);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Could not load reading list assignment targets: " + e.getMessage());
        }
    }

    @GetMapping("/assignment-targets/students")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    public ResponseEntity<?> searchAssignmentStudents(
            @RequestParam(defaultValue = "") String query,
            Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            return ResponseEntity.ok(readingListService.searchAssignmentStudents(uid, query));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Could not search reading list assignment students: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getListDetail(@PathVariable Long id, Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            ReadingListDetailDTO detail = readingListService.getListDetail(id, uid);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not load reading list: " + e.getMessage());
        }
    }

    @GetMapping("/shared/{publicUid}")
    public ResponseEntity<?> getPublicListDetail(@PathVariable String publicUid) {
        try {
            PublicReadingListDetailDTO detail = readingListService.getPublicListDetail(publicUid);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Publieke leeslijst niet gevonden");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not load public reading list: " + e.getMessage());
        }
    }

    @PostMapping("/class")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    public ResponseEntity<?> createClassList(@RequestBody CreateReadingListDTO dto, Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            ReadingListEntity created = readingListService.createClassList(dto, uid);
            return ResponseEntity.ok(created.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating class reading list: " + e.getMessage());
        }
    }

    @PostMapping("/personal")
    public ResponseEntity<?> createPersonalList(@RequestBody CreateReadingListDTO dto, Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            ReadingListEntity created = readingListService.createPersonalList(dto, uid);
            return ResponseEntity.ok(created.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating personal reading list: " + e.getMessage());
        }
    }

    @PutMapping("/personal/{id}")
    public ResponseEntity<?> updatePersonalList(
            @PathVariable Long id,
            @RequestBody CreateReadingListDTO dto,
            Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            ReadingListEntity updated = readingListService.updatePersonalList(id, dto, uid);
            return ResponseEntity.ok(updated.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating personal reading list: " + e.getMessage());
        }
    }

    @PatchMapping("/personal/{id}/visibility")
    public ResponseEntity<?> updatePersonalListVisibility(
            @PathVariable Long id,
            @RequestBody UpdateReadingListVisibilityDTO dto,
            Authentication authentication) {
        try {
            String uid = extractUid(authentication);

            ReadingListVisibilityDTO updated = readingListService.updatePersonalListVisibility(
                    id,
                    dto.publicVisible(),
                    uid);

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating reading list visibility: " + e.getMessage());
        }
    }

    @DeleteMapping("/personal/{id}")
    public ResponseEntity<?> deletePersonalList(@PathVariable Long id, Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            readingListService.deletePersonalList(id, uid);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting personal reading list: " + e.getMessage());
        }
    }

    @DeleteMapping("/class/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    public ResponseEntity<?> deleteClassList(@PathVariable Long id, Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            readingListService.deleteClassList(id, uid);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting class reading list: " + e.getMessage());
        }
    }

    @PutMapping("/class/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    public ResponseEntity<?> updateClassList(@PathVariable Long id, @RequestBody CreateReadingListDTO dto,
            Authentication authentication) {
        try {
            String uid = extractUid(authentication);
            ReadingListEntity updated = readingListService.updateClassList(id, dto, uid);
            return ResponseEntity.ok(updated.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating class reading list: " + e.getMessage());
        }
    }

    /**
     * This helper method extracts the authenticated user's unique ID from the
     * Spring
     * Security Authentication object.
     * It assumes that the user is authenticated via OAuth2 and that the principal
     * contains a "userID" attribute.
     * 
     * @param authentication
     * @return the authenticated user's unique ID
     * @throws IllegalArgumentException if the user is not authenticated
     */

    private String extractUid(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User principal)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        String uid = principal.getAttribute("userID");
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("Authenticated user ID not found");
        }
        return uid;
    }
}