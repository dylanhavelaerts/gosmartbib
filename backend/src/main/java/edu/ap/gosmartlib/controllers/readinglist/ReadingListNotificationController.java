package edu.ap.gosmartlib.controllers.readinglist;

import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.ReadingListService;
import edu.ap.gosmartlib.services.messages.BookNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-controller voor bulknotificaties op alle boeken binnen één leeslijst.
 */
/**
 * Beheert beschikbaarheidsmeldingen voor alle boeken in een leeslijst tegelijk.
 * GET geeft true terug als de gebruiker op elk boek in de lijst geabonneerd is.
 */
@RestController
@RequestMapping("/reading-lists/{readingListId}/notification")
@RequiredArgsConstructor
public class ReadingListNotificationController {

    private final BookNotificationService bookNotificationService;
    private final ReadingListService readingListService;
    private final AuthHelper authHelper;

    /**
     * Controleert of notificaties voor alle boeken uit de leeslijst ingeschakeld
     * zijn.
     *
     * @param readingListId interne leeslijst-ID
     * @param principal     de aangemelde OAuth2-gebruiker
     * @return true wanneer alle boeknotificaties actief zijn
     */
    @GetMapping
    public ResponseEntity<Boolean> status(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User principal) {
        boolean enabled = bookNotificationService.isAllEnabled(authHelper.extractUid(principal),
                getBookIds(readingListId));
        return ResponseEntity.ok(enabled);
    }

    /**
     * Schakelt notificaties in voor alle boeken uit de leeslijst.
     *
     * @param readingListId interne leeslijst-ID
     * @param principal     de aangemelde OAuth2-gebruiker
     * @return lege respons wanneer de notificaties ingeschakeld zijn
     */
    @PostMapping
    public ResponseEntity<Void> enable(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User principal) {
        bookNotificationService.enableBulk(authHelper.extractUid(principal), getBookIds(readingListId));
        return ResponseEntity.ok().build();
    }

    /**
     * Schakelt notificaties uit voor alle boeken uit de leeslijst.
     *
     * @param readingListId interne leeslijst-ID
     * @param principal     de aangemelde OAuth2-gebruiker
     * @return lege respons wanneer de notificaties uitgeschakeld zijn
     */
    @DeleteMapping
    public ResponseEntity<Void> disable(
            @PathVariable Long readingListId,
            @AuthenticationPrincipal OAuth2User principal) {
        bookNotificationService.disableBulk(authHelper.extractUid(principal), getBookIds(readingListId));
        return ResponseEntity.noContent().build();
    }

    private List<Long> getBookIds(Long readingListId) {
        return readingListService.getBookIds(readingListId);
    }

}
