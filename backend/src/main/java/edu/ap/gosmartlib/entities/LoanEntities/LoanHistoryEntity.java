package edu.ap.gosmartlib.entities.loanEntities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tblLoanHistory")
@Getter @Setter @NoArgsConstructor
public class LoanHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String smartschoolUserId;

    @Column(nullable = false)
    private String isbn;

    @Column(nullable = false)
    private int quantity; // Aantal dat is TERUGGEBRACHT

    @Column(nullable = false)
    private LocalDate loanDate; // Oorspronkelijke uitleendatum

    @Column(nullable = false)
    private LocalDate dueDate; // Originele terugbrengdatum

    @Column(nullable = false)
    private LocalDate returnDate; // Datum waarop het is teruggebracht

    @Column(nullable = false)
    private int damagedCount = 0;

    @Column(nullable = false)
    private int brokenCount = 0;

    @Column(nullable = false)
    private int lostCount = 0;
}