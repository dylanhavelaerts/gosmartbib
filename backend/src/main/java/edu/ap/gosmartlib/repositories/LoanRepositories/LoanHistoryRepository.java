package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanHistoryRepository extends JpaRepository<LoanHistoryEntity, Long> {
}