package edu.ap.gosmartlib.dto.schoolintegration;

import java.util.List;
import java.util.Map;

public record SchoolIntegrationLiveClassesResponse(
        boolean success,
        int classCount,
        List<Map<String, Object>> classes,
        String message) {
}