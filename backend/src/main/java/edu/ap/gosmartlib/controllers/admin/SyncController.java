package edu.ap.gosmartlib.controllers.admin;

import edu.ap.gosmartlib.dto.sync.SyncSummaryDTO;
import edu.ap.gosmartlib.services.oneroster.OneRosterSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-controller voor het handmatig starten van de OneRoster-synchronisatie.
 *
 * <p>
 * Deze controller wordt gebruikt door platformbeheerders om alle actieve
 * OneRoster-integraties te synchroniseren. De effectieve synchronisatielogica
 * zit in {@link OneRosterSyncService}.
 * </p>
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final OneRosterSyncService syncService;

    /**
     * Start een volledige OneRoster-synchronisatie voor alle scholen waarvoor
     * OneRoster is ingeschakeld.
     *
     * <p>
     * Alleen platformbeheerders mogen deze actie uitvoeren. De methode geeft
     * een samenvatting terug met het aantal toegevoegde en verwijderde gebruikers
     * per school.
     * </p>
     *
     * @return een overzicht van de uitgevoerde synchronisatie
     */
    @PostMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SyncSummaryDTO syncAll() {
        return syncService.syncAll();
    }

}
