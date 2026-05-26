package edu.ap.gosmartlib.dto.userdirectory;

import java.util.List;
import java.util.Map;

public record ResolveDisplayNamesResponse(
        boolean success,
        int requestedCount,
        int resolvedCount,
        Map<String, String> displayNames,
        List<String> unresolvedUids,
        String message) {
}