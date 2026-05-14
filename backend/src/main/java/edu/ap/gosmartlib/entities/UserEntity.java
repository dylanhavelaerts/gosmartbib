package edu.ap.gosmartlib.entities;

import edu.ap.gosmartlib.util.UserRoles;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tblUsers")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "smartschool_user_id", nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private String smartschoolUid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolEntity school;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private UserRoles role = UserRoles.STUDENT;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tbl_user_classes", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "class_id"))
    private Set<SchoolClassEntity> classes = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ReviewEntity> reviews = new ArrayList<>();

    @Column(name = "oneroster_sourced_id", unique = true)
    private String onerosterSourcedId;
}