package edu.ap.gosmartlib.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Databaseentiteit voor platformadministrator.
 * Admins staan los van Smartschoolgebruikers en hebben hun eigen loginflow via
 * /admin/login. Het wachtwoord wordt altijd opgeslagen als BCrypt-hash.
 */
@Entity
@Table(name = "app_admin")
@Getter
@Setter
@NoArgsConstructor
public class AdminEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}