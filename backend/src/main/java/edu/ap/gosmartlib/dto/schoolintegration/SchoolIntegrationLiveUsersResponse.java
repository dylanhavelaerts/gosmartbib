package edu.ap.gosmartlib.dto.schoolintegration;

import java.util.List;
import java.util.Map;

public record SchoolIntegrationLiveUsersResponse(
        boolean success,
        int userCount,
        List<Map<String, Object>> users,
        String message) {
}