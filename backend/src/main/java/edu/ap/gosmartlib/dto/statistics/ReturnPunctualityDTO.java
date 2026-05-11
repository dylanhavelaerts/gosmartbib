package edu.ap.gosmartlib.dto.statistics;

public record ReturnPunctualityDTO(
        long onTimeCount,
        long lateCount,
        long extensionApprovedCount,
        long extensionPendingCount,
        long extensionDeniedCount
) {}
