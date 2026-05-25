<<<<<<<< HEAD:backend/src/main/java/edu/ap/gosmartlib/entities/schoolEntities/SchoolEntity.java
package edu.ap.gosmartlib.entities.schoolEntities;
========
package edu.ap.gosmartlib.entities.school;
>>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca:backend/src/main/java/edu/ap/gosmartlib/entities/school/SchoolEntity.java

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tblSchools")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class SchoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    // Zelfde scholendomein????
    @Column(nullable = false, unique = true)
    private String domain; // !!! "go-antwerpen.smartschool.be" of "school1.smartschool.be"

    @Column(nullable = false, name = "admin_approved")
    private boolean adminApproved = false;
}
