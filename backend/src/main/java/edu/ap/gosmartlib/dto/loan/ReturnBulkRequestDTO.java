package edu.ap.gosmartlib.dto.loan;

public record ReturnBulkRequestDTO(
    Long bookId, 
    int quantity, 
    String smartschoolUserId
) {}