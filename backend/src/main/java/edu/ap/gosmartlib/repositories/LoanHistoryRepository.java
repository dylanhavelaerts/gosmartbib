package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.LoanHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanHistoryRepository extends JpaRepository<LoanHistoryEntity, Long> {
}