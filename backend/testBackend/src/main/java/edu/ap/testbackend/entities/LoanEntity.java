package edu.ap.testbackend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "tblLoans")
public class LoanEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relatie met jullie bestaande BookEntity
    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    // Relatie met de nieuwe SmartschoolUserEntity
    @ManyToOne(optional = false)
    @JoinColumn(name = "smartschool_user_id", nullable = false)
    private SmartschoolUserEntity user;

    @Column(nullable = false)
    private LocalDate loanDate;

    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    
    public LoanEntity() {
    }
}