package edu.ap.gosmartlib.util;

import java.util.Map;

public class OneRosterUtils {

    private OneRosterUtils() {}

    public static String extractSmartschoolUid(Map<String, Object> onerosterUser) {
        Object metadata = onerosterUser.get("metadata");
        if (metadata instanceof Map<?, ?> meta) {
            Object uid = meta.get("smsc.legacyIdentifier");
            if (uid instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }
}
