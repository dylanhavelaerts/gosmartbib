package edu.ap.testbackend.dto;

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
        Boolean spotlight) {
}