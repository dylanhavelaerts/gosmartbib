package edu.ap.gosmartlib.dto.loan;

import lombok.Data;
import java.time.LocalDate;

public record LoanHistoryDTO (
        Long id,
        String bookTitle,
        String author,
        LocalDate loanDate,
        LocalDate returnDate,
        int quantity,
        int damagedCount,
        int brokenCount,
        int lostCount
) {}