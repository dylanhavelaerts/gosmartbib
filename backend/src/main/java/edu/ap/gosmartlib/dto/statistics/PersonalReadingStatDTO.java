package edu.ap.gosmartlib.dto.statistics;

import java.util.List;

public record PersonalReadingStatDTO (
        int totalBooksRead,
        long totalPagesRead,
        List<String> topGenres
) {}
