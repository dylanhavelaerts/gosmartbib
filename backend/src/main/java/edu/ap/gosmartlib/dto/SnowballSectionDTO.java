package edu.ap.gosmartlib.dto;


import edu.ap.gosmartlib.dto.book.BookDTO;

import java.util.List;

public record SnowballSectionDTO(
        String type,
        String value,
        List<BookDTO> books
) {}
