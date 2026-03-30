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

    @Column(name = "oneroster_sourced_id", unique = true)
    private String onerosterSourcedId;

    @Column(nullable = false, length = 50)
    private String name; // "1A01 B"

    @Column(name = "school_year", length = 20)
    private String schoolYear; // 2025-2026

    @Column(name = "grade", length = 10)
    private String grade; // 1ste jaar

    public SchoolClassEntity(SchoolEntity school, String onerosterSourcedId,
            String name, String schoolYear, String grade) {
        this.school = school;
        this.onerosterSourcedId = onerosterSourcedId;
        this.name = name;
        this.schoolYear = schoolYear;
        this.grade = grade;
    }
}