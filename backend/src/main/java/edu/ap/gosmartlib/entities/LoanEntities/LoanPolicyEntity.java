package edu.ap.gosmartlib.entities.loanEntities;

import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblLoanPolicy")
@Getter @Setter
@NoArgsConstructor
public class LoanPolicyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private SchoolEntity school;

    @Min(1)
    private int defaultLoanPeriodDays;

    @Min(1)
    private int defaultExtensionPeriodDays;

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
