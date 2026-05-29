package edu.ap.gosmartlib.dto.book;

public record BookInventoryDTO(
        Long id,
        Long schoolId,
        String schoolName,
        String campus,
        Integer totalCopies,
        Integer availableCopies,
        Integer damagedCopies,
        Integer brokenCopies,
        Integer lostCopies) {
}