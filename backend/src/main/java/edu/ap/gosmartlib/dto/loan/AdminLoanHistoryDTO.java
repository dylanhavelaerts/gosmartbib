package edu.ap.gosmartlib.dto.loan;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AdminLoanHistoryDTO {
    private Long id;
    private String bookTitle;
    private String author;
    private LocalDate loanDate;
    private LocalDate returnDate;
    private int quantity;
    private String borrowerDisplayName;
    private List<String> borrowerClassNames;
}
