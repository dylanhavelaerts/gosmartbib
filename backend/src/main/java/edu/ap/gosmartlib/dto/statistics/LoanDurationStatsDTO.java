package edu.ap.gosmartlib.dto.statistics;

public record LoanDurationStatsDTO(
        int durationDays,
        long count
) {}
