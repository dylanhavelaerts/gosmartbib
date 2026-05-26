package edu.ap.gosmartlib.dto;

public record BookCopyLabelDTO(
        Long copyId,
        String barcode,
        int copyNumber,
        String bookTitle,
        String isbn,
        String campus,
        String condition
) {}
