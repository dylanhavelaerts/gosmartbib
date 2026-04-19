package edu.ap.gosmartlib.dto.schoolIntegration;

import java.util.List;
import java.util.Map;

public record SchoolIntegrationLiveUsersResponse(
        boolean success,
        int userCount,
        List<Map<String, Object>> users,
        String message) {
}