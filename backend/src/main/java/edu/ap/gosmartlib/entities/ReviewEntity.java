package edu.ap.gosmartlib.entities;

import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.util.ReviewStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Stelt een gebruikersreview voor op een boek.
 * Bij indiening wordt de review automatisch gescand door ReviewAutoModerationService.
 * Afhankelijk van het resultaat krijgt de review status APPROVED of AWAITING_MODERATION.
 * Een bibliotheekbeheerder kan daarna goedkeuren, afkeuren of permanent verwijderen.
 */
@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "tblReviews")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "book_id")
    private BookEntity book;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "is_spoiler")
    private boolean spoiler;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReviewStatus status = ReviewStatus.AWAITING_MODERATION;

    /** Rating van 1 tot en met 5. */
    private float rating;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "review_status")
    private ReviewStatus reviewStatus;

    /** Aantal keer gemeld*/
    @Column(name = "flag_count")
    private int flagCount;

    /** Kommagescheiden lijst van UIDs die deze review gemeld hebben. Voorbeeld: "uid1,uid2,AUTO_MODERATOR" */
    @Column(name = "flagged_by_uids", columnDefinition = "TEXT")
    private String flaggedByUids;

    /** Geserialiseerde meldingsdetails per melder. Formaat: "uid|REDEN;uid2|REDEN2". Voorbeeld: "uid1|SPAM;AUTO_MODERATOR|FOUT_TAALGEBRUIK" */
    @Column(name = "flag_details", columnDefinition = "TEXT")
    private String flagDetails;

    @Column(name = "admin_delete_note", columnDefinition = "TEXT")
    private String adminDeleteNote;

    /** Record blijft in de database maar is niet zichtbaar voor gebruikers. Zie adminDeleteNote voor de optionele reden. */
    @Column(name = "is_admin_deleted", nullable = false)
    private boolean adminDeleted = false;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;
}
