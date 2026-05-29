package edu.ap.gosmartlib.entities.loan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stelt een actieve lening voor.
 * Bij terugbrengen wordt het record verwijderd en verplaatst naar LoanHistoryEntity.
 * De leenperiode wordt bepaald door LoanPolicyEntity van de school van de lener.
 */
@Entity
@Table(name = "tblLoans")
@Getter
@Setter
@NoArgsConstructor
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Smartschool UID van de lener.
     */
    @Column(nullable = true)
    private String smartschoolUserId;

    /**
     * ISBN van het uitgeleende boek.
     */
    @Column(nullable = false)
    private String isbn;

    /**
     * Aantal uitgeleende exemplaren.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Datum waarop het boek is uitgeleend.
     */
    @Column(nullable = false)
    private LocalDate loanDate;

    /**
     * Uiterste terugbrengdatum. Bij goedgekeurde verlenging wordt dit veld opgeschoven.
     */
    @Column(nullable = false)
    private LocalDate dueDate;

    /**
     * Status van de verlengingsaanvraag. NONE als er geen aanvraag is.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanExtensionStatus extensionStatus = LoanExtensionStatus.NONE;

    /**
     * Tijdstip waarop de verlengingsaanvraag werd ingediend.
     */
    private LocalDateTime extensionRequestedAt;

    /**
     * Tijdstip waarop de verlenging werd goedgekeurd of geweigerd.
     */
    private LocalDateTime extensionDecidedAt;

    /**
     * Smartschool UID van de bibliotheekbeheerder die de verlengingsbeslissing nam.
     */
    private String extensionDecidedBySmartschoolUserId;
}