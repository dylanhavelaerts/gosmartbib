package edu.ap.gosmartlib.entities;

import edu.ap.gosmartlib.util.ReadingListType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tblReadingLists")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"books", "creator"})
public class ReadingListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Column(nullable = true)
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 20)
    private ReadingListType listType;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tbl_reading_list_books",
        joinColumns = @JoinColumn(name = "reading_list_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<BookEntity> books = new HashSet<>();
}