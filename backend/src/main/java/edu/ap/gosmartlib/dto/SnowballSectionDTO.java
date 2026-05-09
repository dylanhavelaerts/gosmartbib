package edu.ap.gosmartlib.dto;


import java.util.List;

public record SnowballSectionDTO(
        String type,
        String value,
        List<BookDTO> books
) {}
