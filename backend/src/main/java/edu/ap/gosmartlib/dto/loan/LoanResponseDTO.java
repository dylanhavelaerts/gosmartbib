package edu.ap.gosmartlib.dto.loan;

import java.time.LocalDate;

public class LoanResponseDTO {
    private Long id;
    private String bookTitle;
    private String author;
    private LocalDate borrowDate;
    private LocalDate returnDate;
    private boolean isReturned;

    public LoanResponseDTO(Long id, String bookTitle, String author, LocalDate borrowDate, LocalDate returnDate, boolean isReturned) {
        this.id = id;
        this.bookTitle = bookTitle;
        this.author = author;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
        this.isReturned = isReturned;
    }

    // Getters
    public Long getId() { return id; }
    public String getBookTitle() { return bookTitle; }
    public String getAuthor() { return author; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public boolean isReturned() { return isReturned; }
}