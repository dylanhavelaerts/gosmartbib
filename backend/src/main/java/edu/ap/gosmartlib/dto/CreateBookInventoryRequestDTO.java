package edu.ap.gosmartlib.dto;

public record CreateBookInventoryRequestDTO(
        Long schoolId,
        String campus,
        Integer totalCopies,
        Integer availableCopies) {
}