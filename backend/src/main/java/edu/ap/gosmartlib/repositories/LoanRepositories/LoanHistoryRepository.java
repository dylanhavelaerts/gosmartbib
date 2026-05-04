package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanHistoryRepository extends JpaRepository<LoanHistoryEntity, Long> {
    // Haalt de uitleengeschiedenis op voor een specifieke gebruiker
    List<LoanHistoryEntity> findBySmartschoolUserIdOrderByReturnDateDesc(String smartschoolUserId);
}