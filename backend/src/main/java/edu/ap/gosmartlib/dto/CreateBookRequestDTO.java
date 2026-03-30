package edu.ap.gosmartlib.dto;

import java.util.List;

public record CreateBookRequestDTO(
        String title,
        List<String> authors,
        String publisher,
        String description,
        Integer pageCount,
        List<String> categories,
        String thumbnail,
        String language,
        Double rating,
        Integer publishedYear,
        Boolean spotlight,
        boolean didacticTag,
        List<String> labels,
        String readingLevel,
        Integer totalCopies,
        Integer availableCopies,
        String ageRange) {
}