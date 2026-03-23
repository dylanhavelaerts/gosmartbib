package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.LoanHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanHistoryRepository extends JpaRepository<LoanHistoryEntity, Long> {
}