package edu.ap.gosmartlib.dto.schoolIntegration;

public record UpsertSchoolIntegrationRequest(
                String onerosterBaseUrl,
                String onerosterClientId,
                String onerosterClientSecret,
                Boolean onerosterEnabled,
                String smartschoolAccesscode
) {
}