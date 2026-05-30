package edu.ap.gosmartlib.security;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bijhoudt en beperkt het aantal inlogpogingen per IP-adres.
 * Maximaal 5 pogingen per IP binnen een tijdvenster van 15 minuten.
 * Na een geslaagde login wordt de teller gereset. Verlopen records worden
 * elke 10 minuten automatisch verwijderd.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * Registreert een inlogpoging en controleert of het limiet bereikt is.
     * @param ip het IP-adres van de aanvraag
     * @return true als de poging toegelaten is, false als de limiet overschreden is
     */
    public boolean tryConsume(String ip) {
        long now = System.currentTimeMillis();
        AttemptRecord record = attempts.compute(ip, (key, existing) -> {
            if (existing == null || now - existing.windowStart() > BLOCK_DURATION.toMillis()) {
                return new AttemptRecord(1, now);
            }
            return new AttemptRecord(existing.count() + 1, existing.windowStart());
        });
        return record.count() <= MAX_ATTEMPTS;
    }

    /**
     * Verwijdert verlopen IP-records. Wordt elke 10 minuten automatisch uitgevoerd.
     */
    @Scheduled(fixedRate = 600_000)
    public void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> now - e.getValue().windowStart() > BLOCK_DURATION.toMillis());
    }

    /**
     * Verwijdert de teller van een IP na een geslaagde login.
     * @param ip het IP-adres waarvoor de teller gereset wordt
     */
    public void reset(String ip) {
        attempts.remove(ip);
    }

    private record AttemptRecord(int count, long windowStart) {}
}
