package edu.ap.gosmartlib.dto.schoolIntegration;

import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;

import java.time.LocalDateTime;

public record SchoolIntegrationDTO(
        Long schoolId,
        String schoolBaseUrl,
        String onerosterClientId,
        boolean onerosterEnabled,
        boolean clientSecretConfigured,
        boolean smartschoolAccesscodeConfigured,
        String smartschoolSenderIdentifier,
        LocalDateTime lastTestSuccessfulAt,
        LocalDateTime lastSyncAt,
        String lastError) {
    public static SchoolIntegrationDTO from(SchoolIntegrationEntity entity) {
        return new SchoolIntegrationDTO(
                entity.getSchool().getId(),
                entity.getSchoolBaseUrl(),
                entity.getOnerosterClientId(),
                entity.isOnerosterEnabled(),
                entity.getOnerosterClientSecret() != null && !entity.getOnerosterClientSecret().isBlank(),
                entity.getSmartschoolAccesscode() != null && !entity.getSmartschoolAccesscode().isBlank(),
                entity.getSmartschoolSenderIdentifier(),
                entity.getLastTestSuccessfulAt(),
                entity.getLastSyncAt(),
                entity.getLastError());
    }
}