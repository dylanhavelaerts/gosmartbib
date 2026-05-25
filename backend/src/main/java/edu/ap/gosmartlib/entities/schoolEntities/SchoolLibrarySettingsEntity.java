package edu.ap.gosmartlib.entities.schoolEntities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblSchoolLibrarySettings")
@Getter
@Setter
@NoArgsConstructor
public class SchoolLibrarySettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private SchoolEntity school;

    @Column(name = "barcodes_enabled")
    private boolean barcodesEnabled = false;

    public SchoolLibrarySettingsEntity(SchoolEntity school) {
        this.school = school;
    }
}
