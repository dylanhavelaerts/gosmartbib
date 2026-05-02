package edu.ap.gosmartlib.dto.readinglist;

import java.util.List;

public record ReadingListBookDTO(
        Long id,
        String title,
        List<String> authors,
        String thumbnail,
        String isbn,
        int availableCopies) {
}