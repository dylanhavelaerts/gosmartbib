package edu.ap.gosmartlib.dto.statistics;

public record LoansPerMonthDTO(
        int year,
        int month,
        long count
) {}
