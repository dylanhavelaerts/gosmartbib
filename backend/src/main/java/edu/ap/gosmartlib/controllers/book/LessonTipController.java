package edu.ap.gosmartlib.controllers.book;

import edu.ap.gosmartlib.dto.lessontip.CreateLessonTipDTO;
import edu.ap.gosmartlib.dto.lessontip.LessonTipDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.LessonTipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LessonTipController {
    private final LessonTipService lessonTipService;
    private final AuthHelper authHelper;

    @GetMapping("/books/{bookId}/lesson-tips")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<LessonTipDTO>> getByBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(lessonTipService.getByBookId(bookId, authHelper.extractUid(principal)));
    }

    @PostMapping("/books/{bookId}/lesson-tips")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<LessonTipDTO> create(
            @PathVariable Long bookId,
            @RequestBody CreateLessonTipDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonTipService.create(bookId, authHelper.extractUid(principal), dto));
    }

    @PutMapping("/lesson-tips/{id}")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<LessonTipDTO> update(
            @PathVariable Long id,
            @RequestBody CreateLessonTipDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(lessonTipService.update(id, authHelper.extractUid(principal), dto));
    }

    @DeleteMapping("/lesson-tips/{id}")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {
        lessonTipService.delete(id, authHelper.extractUid(principal));
        return ResponseEntity.noContent().build();
    }
}
