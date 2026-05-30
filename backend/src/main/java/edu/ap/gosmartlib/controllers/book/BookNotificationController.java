package edu.ap.gosmartlib.controllers.book;

import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.messages.BookNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

/**
 * Beheert beschikbaarheidsmeldingen per boek voor de ingelogde gebruiker.
 * De gebruikers-UID wordt afgeleid van het OAuth-principal, nooit uit requestparameters.
 */
@RestController
@RequestMapping("/books/{bookId}/notification")
@RequiredArgsConstructor
public class BookNotificationController {

    private final BookNotificationService bookNotificationService;
    private final AuthHelper authHelper;

    @PostMapping
    public ResponseEntity<Void> enable(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User principal) {
        bookNotificationService.enable(authHelper.extractUid(principal), bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> disable(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User principal) {
        bookNotificationService.disable(authHelper.extractUid(principal), bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Boolean> status(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User principal) {
        boolean enabled = bookNotificationService.isEnabled(authHelper.extractUid(principal), bookId);
        return ResponseEntity.ok(enabled);
    }
}
