package edu.ap.gosmartlib.dto.loan;

import java.time.LocalDate;
import java.util.List;

public record AdminActiveLoanDTO(
        Long loanId,
        String smartschoolUserId,
        String borrowerDisplayName,
        List<String> borrowerClassNames,
        int quantity,
        LocalDate loanDate,
        LocalDate dueDate,
        String extensionStatus,
        LoanBookDTO book) {

    public record LoanBookDTO(
            Long id,
            String title,
            String thumbnail,
            String isbn,
            List<String> authors) {
    }
}
