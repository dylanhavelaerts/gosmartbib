package edu.ap.gosmartlib.dto.reviews;

public record ReviewRequestDTO(
        String bookIsbn,
        String text,
        float rating,
        boolean spoiler
){}
