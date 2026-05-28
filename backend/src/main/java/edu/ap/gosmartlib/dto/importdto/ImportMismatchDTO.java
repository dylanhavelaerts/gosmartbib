package edu.ap.gosmartlib.dto.importdto;

public record ImportMismatchDTO(
                int rowNumber,
                String isbn,
                String excelTitle,
                String fetchedTitle,
                String reason,
                Integer amount,
                Boolean didacticBook) {

        public ImportMismatchDTO(
                        int rowNumber,
                        String isbn,
                        String excelTitle,
                        String fetchedTitle,
                        String reason) {
                this(rowNumber, isbn, excelTitle, fetchedTitle, reason, null, null);
        }

        public ImportMismatchDTO(
                        int rowNumber,
                        String isbn,
                        String excelTitle,
                        String fetchedTitle,
                        String reason,
                        Integer amount) {
                this(rowNumber, isbn, excelTitle, fetchedTitle, reason, amount, null);
        }
}