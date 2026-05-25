<<<<<<<< HEAD:backend/src/main/java/edu/ap/gosmartlib/entities/schoolEntities/SchoolCampusEntity.java
package edu.ap.gosmartlib.entities.schoolEntities;
========
package edu.ap.gosmartlib.entities.school;
>>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca:backend/src/main/java/edu/ap/gosmartlib/entities/school/SchoolCampusEntity.java

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "tblSchoolCampuses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_school_campus_name",
                        columnNames = {"school_id", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchoolCampusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolEntity school;

    @Column(nullable = false, length = 120)
    @ToString.Include
    private String name;
}