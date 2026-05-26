package edu.ap.gosmartlib.dto.schoolIntegration;

public record UpsertSchoolIntegrationRequest(
                String schoolBaseUrl,
                String onerosterClientId,
                String onerosterClientSecret,
                Boolean onerosterEnabled,
                String smartschoolAccesscode
) {
}