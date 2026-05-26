package edu.ap.gosmartlib.repositories.loan;

import edu.ap.gosmartlib.entities.loan.LoanPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanPolicyRepository extends JpaRepository<LoanPolicyEntity, Long> {
    Optional<LoanPolicyEntity> findBySchool_Id(Long schoolId);
}
