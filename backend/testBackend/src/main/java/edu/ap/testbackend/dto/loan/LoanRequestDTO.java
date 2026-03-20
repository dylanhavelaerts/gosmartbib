package edu.ap.testbackend.dto.loan;
public record LoanRequestDTO(Long bookId, int quantity, SmartschoolUserDTO user) {}