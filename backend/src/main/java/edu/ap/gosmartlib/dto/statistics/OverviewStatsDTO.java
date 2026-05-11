package edu.ap.gosmartlib.dto.statistics;

public record OverviewStatsDTO(
        long activeLoans,
        long overdueLoans,
        long inactiveStudents,
        long pendingExtensions
) {}
