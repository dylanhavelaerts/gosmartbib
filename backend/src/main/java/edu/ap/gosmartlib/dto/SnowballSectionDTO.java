package edu.ap.gosmartlib.dto;


import java.util.List;

public record SnowballSectionDTO(
        String title,
        String filterLabel,
        List<BookDTO> books
) {}
