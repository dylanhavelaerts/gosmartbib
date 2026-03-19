package edu.ap.gosmartlib.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tblSchoolClasses")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class SchoolClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolEntity school;

    @Column(nullable = false, length = 50)
    private String name; // "3A", "3LAT", ...

    @Column(name = "academic_year", length = 50)
    private String academicYear; // 2de jaar ("2")

    public SchoolClassEntity(SchoolEntity school, String name, String academicYear) {
        this.school = school;
        this.name = name;
        this.academicYear = academicYear;
    }
}
