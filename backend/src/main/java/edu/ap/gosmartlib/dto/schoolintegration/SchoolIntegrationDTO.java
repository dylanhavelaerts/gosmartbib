package edu.ap.gosmartlib.dto.schoolintegration;

import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;

import java.time.LocalDateTime;

public record SchoolIntegrationDTO(
        Long schoolId,
        String schoolName,
        String schoolDomain,
        String schoolBaseUrl,
        String onerosterClientId,
        boolean onerosterEnabled,
        boolean clientSecretConfigured,
        boolean smartschoolAccesscodeConfigured,
        LocalDateTime lastTestSuccessfulAt,
        LocalDateTime lastSyncAt,
        String lastError) {
    public static SchoolIntegrationDTO from(SchoolIntegrationEntity entity) {
        return new SchoolIntegrationDTO(
                entity.getSchool().getId(),
                entity.getSchool().getName(),
                entity.getSchool().getDomain(),
                entity.getSchoolBaseUrl(),
                entity.getOnerosterClientId(),
                entity.isOnerosterEnabled(),
                entity.getOnerosterClientSecret() != null && !entity.getOnerosterClientSecret().isBlank(),
                entity.getSmartschoolAccesscode() != null && !entity.getSmartschoolAccesscode().isBlank(),
                entity.getLastTestSuccessfulAt(),
                entity.getLastSyncAt(),
                entity.getLastError());
    }
}