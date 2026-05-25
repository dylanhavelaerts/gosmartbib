package edu.ap.gosmartlib.entities;

import edu.ap.gosmartlib.entities.bookEntities.BookEntity;
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.util.ReadingListTargetType;
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
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tblReadingLists")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "books", "creator", "targetStudents", "targetClasses" })
public class ReadingListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Column(name = "public_uid", unique = true, length = 36)
    private String publicUid;

    @Column(name = "public_visible", nullable = false)
    private boolean publicVisible = false;

    @Column(nullable = true)
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnore
    private UserEntity creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 20)
    private ReadingListType listType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private ReadingListTargetType targetType;

    @ManyToMany
    @JoinTable(name = "reading_list_target_students", joinColumns = @JoinColumn(name = "reading_list_id"), inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<UserEntity> targetStudents = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "reading_list_target_classes", joinColumns = @JoinColumn(name = "reading_list_id"), inverseJoinColumns = @JoinColumn(name = "class_id"))
    private Set<SchoolClassEntity> targetClasses = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "reading_list_target_years", joinColumns = @JoinColumn(name = "reading_list_id"))
    @Column(name = "year_value")
    private Set<Integer> targetYears = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "reading_list_target_grades", joinColumns = @JoinColumn(name = "reading_list_id"))
    @Column(name = "grade_value")
    private Set<Integer> targetGrades = new HashSet<>();

    @Column(name = "target_all_schools", nullable = false)
    private boolean targetAllSchools = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tbl_reading_list_books", joinColumns = @JoinColumn(name = "reading_list_id"), inverseJoinColumns = @JoinColumn(name = "book_id"))
    private Set<BookEntity> books = new HashSet<>();

    @PrePersist
    private void ensurePublicUidBeforePersist() {
        if (publicUid == null || publicUid.isBlank()) {
            publicUid = UUID.randomUUID().toString();
        }
    }
}