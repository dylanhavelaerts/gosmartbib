package edu.ap.gosmartlib.dto.loan;

import java.time.LocalDate;

public record ActiveLoanDTO(
    Long loanId,
    String smartschoolUserId,
    int quantity,
    LocalDate loanDate,
    LocalDate dueDate,
    LoanBookDTO book
) {
    public record LoanBookDTO(
        Long id, 
        String title, 
        String thumbnail, 
        String isbn
    ) {}
}