package edu.ap.gosmartlib.dto.book;

public record CreateBookInventoryRequestDTO(
        Long schoolId,
        String campus,
        Integer totalCopies,
        Integer availableCopies) {
}