package edu.ap.gosmartlib.dto.loan;

public record LoanRequestDTO(Long bookId, int quantity, SmartschoolUserDTO user) {}