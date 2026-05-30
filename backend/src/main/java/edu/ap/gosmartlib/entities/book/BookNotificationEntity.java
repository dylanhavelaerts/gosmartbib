package edu.ap.gosmartlib.entities.book;

import edu.ap.gosmartlib.entities.UserEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Abonnementsrecord dat bijhoudt welke gebruiker een melding wil ontvangen als een boek beschikbaar wordt.
 * De unieke constraint op (user_id, book_id) voorkomt dubbele abonnementen.
 * Abonnementen zijn eenmalig: ze worden verwijderd na het versturen van de melding.
 */
@Entity
@Table(name = "tbl_book_notifications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;
}
