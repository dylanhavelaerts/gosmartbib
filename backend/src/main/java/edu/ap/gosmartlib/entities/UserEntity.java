package edu.ap.gosmartlib.entities;

import edu.ap.gosmartlib.util.UserRoles;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    // Soft delete veld
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Als een gebruiker gedeactiveerd is,
    // kunnen we de datum van deactivering en de geplande verwijderdatum bijhouden
    // Later nog bij de klant horen hoe lang dit moet zijn (bv. 1 jaar na
    // deactivering)
    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "scheduled_deletion_at")
    private LocalDateTime scheduledDeletionAt;

    public void deactivate(long retentionDays) {
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
        this.scheduledDeletionAt = this.deactivatedAt.plusDays(retentionDays);
    }

    public void reactivate() {
        this.active = true;
        this.deactivatedAt = null;
        this.scheduledDeletionAt = null;
    }
}