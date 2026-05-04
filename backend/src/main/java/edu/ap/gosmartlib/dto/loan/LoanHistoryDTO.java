package edu.ap.gosmartlib.dto.loan;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LoanHistoryDTO {
    private Long id;
    private String bookTitle;
    private String author;
    private LocalDate loanDate;
    private LocalDate returnDate;
    private int quantity;
}