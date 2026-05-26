package edu.ap.gosmartlib.dto.schoolintegration;

public record UpsertSchoolIntegrationRequest(
                String schoolBaseUrl,
                String onerosterClientId,
                String onerosterClientSecret,
                Boolean onerosterEnabled,
                String smartschoolAccesscode
) {
}