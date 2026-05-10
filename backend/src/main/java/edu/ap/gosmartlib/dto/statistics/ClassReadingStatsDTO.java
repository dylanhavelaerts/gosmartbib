package edu.ap.gosmartlib.dto.statistics;

public record ClassReadingStatsDTO(
   String className,
   String grade,
   String schoolYear,
   long loanCount
) {}
