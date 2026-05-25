package edu.ap.gosmartlib.entities.bookEntities;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tblBookInventories", uniqueConstraints = @UniqueConstraint(name = "uk_book_school_campus", columnNames = {
        "book_id", "school_id", "campus" }))
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookInventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolEntity school;

    @Column(name = "campus", nullable = false)
    private String campus = "";

    @Column(name = "total_copies", nullable = false)
    private Integer totalCopies = 0;

    @Column(name = "available_copies", nullable = false)
    private Integer availableCopies = 0;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookCopyEntity> copies = new ArrayList<>();
}