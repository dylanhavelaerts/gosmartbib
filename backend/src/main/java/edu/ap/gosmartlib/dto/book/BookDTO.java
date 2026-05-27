package edu.ap.gosmartlib.dto.book;

import java.util.List;

public record BookDTO(
                Long id,
                String title,
                List<String> authors,
                String publisher,
                String description,
                Integer pageCount,
                List<String> categories,
                String thumbnail,
                String language,
                Double rating,
                String isbn,
                Integer publishedYear,
                Boolean didacticTag,
                List<String> labels,
                String readingLevel,
                Integer totalCopies,
                Integer availableCopies,
                String ageRange,
                String previewLink,
                List<BookInventoryDTO> inventories) {
}