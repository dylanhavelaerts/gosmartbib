package edu.ap.gosmartlib.entities.loan;

import edu.ap.gosmartlib.entities.school.SchoolEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bevat de leenregels per school.
 * Elke school heeft maximaal één policy. Als er geen policy bestaat, gebruikt LoanService een standaard leenperiode van 14 dagen.
 */
@Entity
@Table(name = "tblLoanPolicy")
@Getter
@Setter
@NoArgsConstructor
public class LoanPolicyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private SchoolEntity school;

    /**
     * Standaard leenperiode in dagen voor deze school.
     */
    @Min(1)
    private int defaultLoanPeriodDays;

    /**
     * Geconfigureerde verlengingsperiode in dagen per school. Momenteel niet gebruikt door de verlengingslogica
     * de verlenging wordt berekend op basis van de originele leenperiode.
     */
    @Min(1)
    private int defaultExtensionPeriodDays;

    /**
     * Aantal dagen voor de vervaldatum waarop een herinnering verstuurd wordt.
     * Standaard 3 dagen, ook ingesteld via de constructor en als SQL-default om te garanderen dat de waarde altijd aanwezig is.
     */
    @Min(1)
    @Column(columnDefinition = "integer default 3 not null")
    private int dueDateReminderDays = 3;

    public LoanPolicyEntity(SchoolEntity school, int defaultLoanPeriodDays, int defaultExtensionPeriodDays) {
        this.school = school;
        this.defaultLoanPeriodDays = defaultLoanPeriodDays;
        this.defaultExtensionPeriodDays = defaultExtensionPeriodDays;
        this.dueDateReminderDays = 3;
    }

}
