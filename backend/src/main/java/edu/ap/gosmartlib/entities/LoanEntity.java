package edu.ap.testbackend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tblLoans")
@Getter @Setter @NoArgsConstructor
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String smartschoolUserId; // Gebruikers ID

    @Column(nullable = false)
    private String isbn; // ISBN nummer per uitleen

    @Column(nullable = false)
    private int quantity; // Aantal uitgeleende exemplaren

    @Column(nullable = false)
    private LocalDate loanDate; // Datum van uitleen

    @Column(nullable = false)
    private LocalDate dueDate; // Ten laatste terugbrengen
}