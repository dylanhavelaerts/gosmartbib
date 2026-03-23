package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {
    List<LoanEntity> findBySmartschoolUserId(String smartschoolUserId);
}