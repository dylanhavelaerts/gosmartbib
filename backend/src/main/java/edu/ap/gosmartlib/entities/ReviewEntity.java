package edu.ap.gosmartlib.entities;

import edu.ap.gosmartlib.entities.BookEntities.BookEntity;
import edu.ap.gosmartlib.util.ReviewStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

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

    private float rating; // 1 - 5

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "review_status")
    private ReviewStatus reviewStatus;

    @Column(name = "flag_count")
    private int flagCount; // Aantal keren gemeld

    @Column(name = "flagged_by_uids", columnDefinition = "TEXT")
    private String flaggedByUids;

    @Column(name = "flag_details", columnDefinition = "TEXT")
    private String flagDetails;

    @Column(name = "admin_delete_note", columnDefinition = "TEXT")
    private String adminDeleteNote;

    @Column(name = "is_admin_deleted", nullable = false)
    private boolean adminDeleted = false;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;
}
