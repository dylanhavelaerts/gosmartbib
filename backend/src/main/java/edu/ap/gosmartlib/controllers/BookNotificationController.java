package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.services.BookNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/books/{bookId}/notification")
@RequiredArgsConstructor
public class BookNotificationController {

    private final BookNotificationService bookNotificationService;

    @PostMapping
    public ResponseEntity<Void> enable(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        bookNotificationService.enable(extractUid(oAuth2User), bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> disable(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        bookNotificationService.disable(extractUid(oAuth2User), bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Boolean> status(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        boolean enabled = bookNotificationService.isEnabled(extractUid(oAuth2User), bookId);
        return ResponseEntity.ok(enabled);
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
