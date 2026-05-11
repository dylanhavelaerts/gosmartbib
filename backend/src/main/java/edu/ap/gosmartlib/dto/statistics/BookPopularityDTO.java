package edu.ap.gosmartlib.dto.statistics;

import java.util.List;

public record BookPopularityDTO(
        String isbn,
        String title,
        List<String> authors,
        long loanCount
) {}
