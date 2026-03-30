package edu.ap.gosmartlib.entities;

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

    private int rating; // 1 - 5

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "review_status")
    private ReviewStatus reviewStatus;

    @Column(name = "flag_count")
    private int flagCount; // Aantal keren gemeld
}
