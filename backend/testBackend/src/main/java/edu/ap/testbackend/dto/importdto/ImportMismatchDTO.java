package edu.ap.testbackend.dto.importdto;

public record ImportMismatchDTO(
        int rowNumber,
        String isbn,
        String excelTitle,
        String fetchedTitle,
        String reason) {
}
