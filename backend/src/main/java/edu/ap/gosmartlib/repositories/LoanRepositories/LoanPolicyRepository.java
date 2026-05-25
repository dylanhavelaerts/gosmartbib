package edu.ap.gosmartlib.repositories.loanRepositories;

import edu.ap.gosmartlib.entities.loanEntities.LoanPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanPolicyRepository extends JpaRepository<LoanPolicyEntity, Long> {
    Optional<LoanPolicyEntity> findBySchool_Id(Long schoolId);
}
