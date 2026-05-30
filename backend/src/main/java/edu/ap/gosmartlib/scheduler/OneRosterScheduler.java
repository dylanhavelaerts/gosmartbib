package edu.ap.gosmartlib.scheduler;

import edu.ap.gosmartlib.services.oneroster.OneRosterSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler die de OneRoster-synchronisatie automatisch uitvoert.
 *
 * <p>
 * Deze component zorgt ervoor dat gebruikers, klassen en inschrijvingen
 * periodiek opnieuw worden opgehaald uit Smartschool OneRoster.
 * </p>
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OneRosterScheduler {

    private final OneRosterSyncService syncService;

    /**
     * Voert de jaarlijkse OneRoster-synchronisatie uit.
     *
     * <p>
     * De sync wordt automatisch gestart volgens de ingestelde cron-expressie.
     * Dit is bedoeld om bij de start van een nieuw schooljaar de gebruikers,
     * klassen en inschrijvingen opnieuw gelijk te trekken met Smartschool.
     * </p>
     */

    @Scheduled(cron = "0 0 2 30 8 *")
    public void yearlySync() {
        log.info("Starting yearly OneRoster sync");
        syncService.syncAll();
        log.info("Yearly OneRoster sync completed");
    }
}
