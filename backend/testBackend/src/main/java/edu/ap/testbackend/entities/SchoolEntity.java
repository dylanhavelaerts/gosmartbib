package edu.ap.testbackend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="tblSchools")
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class SchoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    // Zelfe scholendomein????
    @Column(nullable = false, unique = true)
    private String domain; // !!! "go-antwerpen.smartschool.be" of "school1.smartschool.be"
}
