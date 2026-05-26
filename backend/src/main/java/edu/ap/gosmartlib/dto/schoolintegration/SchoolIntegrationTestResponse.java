package edu.ap.gosmartlib.dto.schoolintegration;

public record SchoolIntegrationTestResponse(
                boolean success,
                boolean tokenReceived,
                boolean schoolsEndpointReachable,
                int schoolCount,
                String message) {
}