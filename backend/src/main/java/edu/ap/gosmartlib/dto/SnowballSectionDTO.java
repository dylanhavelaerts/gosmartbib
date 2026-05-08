package edu.ap.gosmartlib.dto;


import java.util.List;

public record SnowballSectionDTO(
        String title,
        List<BookDTO> books
) {}
