package edu.ap.gosmartlib.security;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

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
    @Scheduled(fixedRate = 600_000)
    public void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> now - e.getValue().windowStart() > BLOCK_DURATION.toMillis());
    }

    public void reset(String ip) {
        attempts.remove(ip);
    }

    private record AttemptRecord(int count, long windowStart) {}
}
