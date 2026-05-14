package edu.ap.gosmartlib.entities.LoanEntities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblLoans")
@Getter
@Setter
@NoArgsConstructor
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String smartschoolUserId; // Gebruikers ID

    @Column(nullable = false)
    private String isbn; // ISBN nummer per uitleen

    @Column(nullable = false)
    private int quantity; // Aantal uitgeleende exemplaren

    @Column(nullable = false)
    private LocalDate loanDate; // Datum van uitleen

    @Column(nullable = false)
    private LocalDate dueDate; // Ten laatste terugbrengen

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanExtensionStatus extensionStatus = LoanExtensionStatus.NONE;

    private LocalDateTime extensionRequestedAt;

    private LocalDateTime extensionDecidedAt;

    private String extensionDecidedBySmartschoolUserId;
}