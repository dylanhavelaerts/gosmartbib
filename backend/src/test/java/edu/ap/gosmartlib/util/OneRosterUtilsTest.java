package edu.ap.gosmartlib.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OneRosterUtilsTest {

    @Test
    void givenValidLegacyIdentifier_whenExtractSmartschoolUid_thenReturnsUid() {
        Map<String, Object> user = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "student-uid-123"));

        String result = OneRosterUtils.extractSmartschoolUid(user);

        assertEquals("student-uid-123", result);
    }

    @Test
    void givenNoMetadata_whenExtractSmartschoolUid_thenReturnsNull() {
        Map<String, Object> user = Map.of("identifier", "some-id");

        String result = OneRosterUtils.extractSmartschoolUid(user);

        assertNull(result);
    }

    @Test
    void givenMetadataWithoutLegacyIdentifier_whenExtractSmartschoolUid_thenReturnsNull() {
        Map<String, Object> user = Map.of(
                "metadata", Map.of("other.key", "value"));

        String result = OneRosterUtils.extractSmartschoolUid(user);

        assertNull(result);
    }

    @Test
    void givenBlankLegacyIdentifier_whenExtractSmartschoolUid_thenReturnsNull() {
        Map<String, Object> user = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "   "));

        String result = OneRosterUtils.extractSmartschoolUid(user);

        assertNull(result);
    }

    @Test
    void givenNullLegacyIdentifier_whenExtractSmartschoolUid_thenReturnsNull() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("smsc.legacyIdentifier", null);
        Map<String, Object> user = Map.of("metadata", metadata);

        String result = OneRosterUtils.extractSmartschoolUid(user);

        assertNull(result);
    }

    @Test
    void givenMetadataIsNotMap_whenExtractSmartschoolUid_thenReturnsNull() {
        Map<String, Object> user = Map.of("metadata", "not-a-map");

        String result = OneRosterUtils.extractSmartschoolUid(user);

        assertNull(result);
    }
}
