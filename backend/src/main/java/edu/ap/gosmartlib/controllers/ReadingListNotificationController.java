package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.entities.BookEntities.BookEntity;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.messages.BookNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/reading-lists/{readingListId}/notification")
@RequiredArgsConstructor
public class ReadingListNotificationController {

    private final BookNotificationService bookNotificationService;
    private final ReadingListRepository readingListRepository;
    private final AuthHelper authHelper;

    @GetMapping
    public ResponseEntity<Boolean> status(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User principal) {
        boolean enabled = bookNotificationService.isAllEnabled(authHelper.extractUid(principal), getBookIds(readingListId));
        return ResponseEntity.ok(enabled);
    }

    @PostMapping
    public ResponseEntity<Void> enable(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User principal) {
        bookNotificationService.enableBulk(authHelper.extractUid(principal), getBookIds(readingListId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> disable(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User principal) {
        bookNotificationService.disableBulk(authHelper.extractUid(principal), getBookIds(readingListId));
        return ResponseEntity.noContent().build();
    }

    private List<Long> getBookIds(Long readingListId) {
        return readingListRepository.findByIdWithBooks(readingListId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leeslijst niet gevonden"))
                .getBooks().stream()
                .map(BookEntity::getId)
                .toList();
    }
}
