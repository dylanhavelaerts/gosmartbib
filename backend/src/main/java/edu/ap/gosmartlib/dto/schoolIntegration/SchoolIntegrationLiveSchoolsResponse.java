package edu.ap.gosmartlib.dto.schoolIntegration;

import java.util.List;
import java.util.Map;

public record SchoolIntegrationLiveSchoolsResponse(
                boolean success,
                int schoolCount,
                List<Map<String, Object>> schools,
                String message) {
}