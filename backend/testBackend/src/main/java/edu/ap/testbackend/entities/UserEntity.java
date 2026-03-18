package edu.ap.testbackend.entities;

import edu.ap.testbackend.util.UserRoles;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="tblUsers")
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolEntity school;

    @Column(name = "smartschool_uid", nullable = false)
    private String smartschoolUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private UserRoles role = UserRoles.STUDENT; // By default, wordt gebruiker als student beschouwd

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_class_id")
    private SchoolClassEntity currentClass; // Nullable, omdat leerkrachten en hoger mogelijk geen klas hebben
}
