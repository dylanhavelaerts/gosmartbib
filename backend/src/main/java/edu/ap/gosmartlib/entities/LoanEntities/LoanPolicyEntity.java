package edu.ap.gosmartlib.entities.loanEntities;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
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
