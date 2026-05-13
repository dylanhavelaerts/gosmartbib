package edu.ap.gosmartlib.dto.statistics;

import java.util.List;

public record MostWantedBookDTO(
        String isbn,
        String title,
        List<String> authors,
        long notificationCount
) {}
