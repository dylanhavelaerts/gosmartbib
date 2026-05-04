package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {
    List<LoanEntity> findBySmartschoolUserId(String smartschoolUserId);

    List<LoanEntity> findBySmartschoolUserIdAndIsbn(String smartschoolUserId, String isbn);

    List<LoanEntity> findByDueDate(LocalDate dueDate);

    @Query("""
            select loan
            from LoanEntity loan
            join UserEntity user on user.smartschoolUid = loan.smartschoolUserId
            where loan.extensionStatus = :status
              and user.school.id = :schoolId
            order by loan.extensionRequestedAt asc
            """)
    List<LoanEntity> findExtensionRequestsForSchool(
            LoanExtensionStatus status,
            Long schoolId);
}