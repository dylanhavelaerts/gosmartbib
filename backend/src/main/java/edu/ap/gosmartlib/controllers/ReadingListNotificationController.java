package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.services.BookNotificationService;
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

    @GetMapping
    public ResponseEntity<Boolean> status(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        String uid = extractUid(oAuth2User);
        List<Long> bookIds = getBookIds(readingListId);
        boolean enabled = bookNotificationService.isAllEnabled(uid, bookIds);
        return ResponseEntity.ok(enabled);
    }

    @PostMapping
    public ResponseEntity<Void> enable(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        bookNotificationService.enableBulk(extractUid(oAuth2User), getBookIds(readingListId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> disable(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        bookNotificationService.disableBulk(extractUid(oAuth2User), getBookIds(readingListId));
        return ResponseEntity.noContent().build();
    }

    private List<Long> getBookIds(Long readingListId) {
        return readingListRepository.findByIdWithBooks(readingListId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leeslijst niet gevonden"))
                .getBooks().stream()
                .map(BookEntity::getId)
                .toList();
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

