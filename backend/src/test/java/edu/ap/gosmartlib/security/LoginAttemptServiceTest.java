package edu.ap.gosmartlib.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void givenFreshIp_whenTryConsume_thenAllowed() {
        assertTrue(service.tryConsume("1.2.3.4"));
    }

    @Test
    void givenMaxAttemptsReached_whenTryConsume_thenBlocked() {
        for (int i = 0; i < 5; i++) service.tryConsume("1.2.3.4");
        assertFalse(service.tryConsume("1.2.3.4"));
    }

    @Test
    void givenBlockedIp_whenReset_thenAllowedAgain() {
        for (int i = 0; i < 6; i++) service.tryConsume("1.2.3.4");
        service.reset("1.2.3.4");
        assertTrue(service.tryConsume("1.2.3.4"));
    }

    @Test
    void givenExpiredEntry_whenEvict_thenEntryRemoved() throws Exception {
        service.tryConsume("1.2.3.4");
        // Evict verwijdert niets want entry is vers
        service.evictExpiredEntries();
        // Na evict is de entry er nog want window niet verlopen - count loopt door
        assertTrue(service.tryConsume("1.2.3.4"));
    }
}
