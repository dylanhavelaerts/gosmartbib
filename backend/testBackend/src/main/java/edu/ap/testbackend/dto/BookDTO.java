package edu.ap.testbackend.dto;

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
                Integer totalCopies,
                Integer availableCopies) {
}