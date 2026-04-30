package edu.ap.gosmartlib.entities;

import edu.ap.gosmartlib.util.PurchaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tblPurchaseRequest")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class PurchaseRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String title;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "book_authors", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "author")
    private List<String> authors;

    private String isbn;

    private PurchaseStatus status = PurchaseStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private LocalDate requestDate = LocalDate.now();

    @Column(columnDefinition = "TEXT")
    private String note;

    public PurchaseRequestEntity(String title, List<String> authors, String isbn, PurchaseStatus status, UserEntity user, LocalDate requestDate, String note) {
        this.title = title;
        this.authors = authors;
        this.isbn = isbn;
        this.status = status;
        this.user = user;
        this.requestDate = requestDate;
        this.note = note;
    }
}
