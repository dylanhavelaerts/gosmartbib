package edu.ap.gosmartlib.entities.loan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Slaat een afgeronde lening op na terugbrengen.
 * Bevat naast de originele leengegevens ook de staat van de exemplaren bij terugbrengen.
 */
@Entity
@Table(name = "tblLoanHistory")
@Getter
@Setter
@NoArgsConstructor
public class LoanHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Smartschool UID van de lener.
     */
    @Column(nullable = true)
    private String smartschoolUserId;

    /**
     * ISBN van het teruggebrachte boek.
     */
    @Column(nullable = false)
    private String isbn;

    /**
     * Aantal teruggebrachte exemplaren.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Originele uitleendatum.
     */
    @Column(nullable = false)
    private LocalDate loanDate;

    /**
     * Originele terugbrengdatum zoals afgesproken bij uitleen.
     */
    @Column(nullable = false)
    private LocalDate dueDate;

    /**
     * Datum waarop de exemplaren effectief teruggebracht werden.
     */
    @Column(nullable = false)
    private LocalDate returnDate;

    /**
     * Aantal exemplaren teruggebracht in beschadigde staat.
     */
    @Column(nullable = false)
    private int damagedCount = 0;

    /**
     * Aantal exemplaren teruggebracht in gebroken staat.
     */
    @Column(nullable = false)
    private int brokenCount = 0;

    /**
     * Aantal exemplaren dat verloren is gegaan.
     */
    @Column(nullable = false)
    private int lostCount = 0;
}