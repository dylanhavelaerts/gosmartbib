package edu.ap.gosmartlib.dto.statistics;

import java.util.List;

public record PersonalReadingStatDTO (
        int totalBooksRead,
        List<String> topGenres
) {}
