package edu.ap.gosmartlib.dto.loan;

import java.time.LocalDate;
import java.util.List;

public record AdminLoanHistoryDTO(
        Long id,
        String bookTitle,
        String author,
        LocalDate loanDate,
        LocalDate returnDate,
        int quantity,
        String borrowerDisplayName,
        List<String> borrowerClassNames
) {}
