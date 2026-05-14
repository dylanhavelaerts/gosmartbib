package edu.ap.gosmartlib.scheduler;

import edu.ap.gosmartlib.services.oneRoster.OneRosterSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OneRosterScheduler {

    private final OneRosterSyncService syncService;

    @Scheduled(cron = "0 0 2 30 8 *")
    public void yearlySync() {
        log.info("Starting yearly OneRoster sync");
        syncService.syncAll();
        log.info("Yearly OneRoster sync completed");
    }
}
