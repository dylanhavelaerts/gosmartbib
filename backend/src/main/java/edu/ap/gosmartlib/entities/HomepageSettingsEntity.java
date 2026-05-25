package edu.ap.gosmartlib.entities;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblHomepageSettings")
@Getter @Setter
@NoArgsConstructor
public class HomepageSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private SchoolEntity school;

    @Column(columnDefinition = "boolean default true")
    private boolean showSpotlight = true;

    @Column(columnDefinition = "boolean default true")
    private boolean showNewInLibrary = true;

    @Column(columnDefinition = "boolean default true")
    private boolean showReadingLists = true;

    @Column(columnDefinition = "boolean default true")
    private boolean showUrgentLoans = true;

    public HomepageSettingsEntity(SchoolEntity school) {
        this.school = school;
    }
}