package edu.ap.gosmartlib.entities.bookEntities;

import edu.ap.gosmartlib.entities.ReviewEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tblBooks")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class BookEntity {
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

    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "page_count")
    private Integer pageCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "book_categories", joinColumns = @JoinColumn(name = "book_id"))
    private List<String> categories;

    private String thumbnail;

    private String language;

    private Double rating;
    
    @Column(unique = true, nullable = false)
    private String isbn;

    @Column(name = "published_year")
    private Integer publishedYear;
    
    @Column(nullable = false)
    private boolean spotlight = false;

    @Column(name = "didactic_tag", nullable = false)
    private boolean didacticTag;

    @Column(name = "reading_level")
    private String readingLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "book_labels", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "label")
    private List<String> labels;

    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer totalCopies = 1;

    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer availableCopies = 1;

    @Column(nullable = true)
    private String ageRange;

    @Column(length = 500)
    private String previewLink;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookInventoryEntity> inventories = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReviewEntity> reviews = new ArrayList<>();

    public BookEntity(String title, List<String> authors, String publisher, String description, int pageCount,
            List<String> categories, String thumbnail, String language, double rating, String isbn,
            Integer publishedYear, boolean didacticTag, List<String> labels, String readingLevel, Integer totalCopies,
            Integer availableCopies, String ageRange) {
        this.title = title;
        this.authors = authors;
        this.publisher = publisher;
        this.description = description;
        this.pageCount = pageCount;
        this.categories = categories;
        this.thumbnail = thumbnail;
        this.language = language;
        this.rating = rating;
        this.spotlight = false;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.didacticTag = didacticTag;
        this.labels = labels;
        this.readingLevel = readingLevel;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.ageRange = ageRange;
    }
}