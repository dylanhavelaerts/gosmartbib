package edu.ap.gosmartlib.dto.schoolIntegration;

public record SchoolIntegrationTestResponse(
                boolean success,
                boolean tokenReceived,
                boolean schoolsEndpointReachable,
                int schoolCount,
                String message) {
}