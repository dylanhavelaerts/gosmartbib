package edu.ap.gosmartlib.dto.loan;

import java.time.LocalDate;

public record ActiveLoanDTO(
    Long loanId, 
    String smartschoolUserId, 
    int quantity, 
    LocalDate loanDate, 
    LoanBookDTO book
) {
    // Een veilige, platte versie van het boek zonder complexe database-relaties
    public record LoanBookDTO(
        Long id, 
        String title, 
        String thumbnail, 
        String isbn
    ) {}
}