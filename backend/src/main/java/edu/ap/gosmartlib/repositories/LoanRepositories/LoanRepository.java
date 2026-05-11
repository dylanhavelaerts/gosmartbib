package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT l.extensionStatus, COUNT(l)
            FROM LoanEntity l
            JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
            WHERE u.school.id = :schoolId AND l.extensionStatus <> :noneStatus
            GROUP BY l.extensionStatus
            """)
    List<Object[]> countExtensionsByStatus(@Param("schoolId") Long schoolId,
                                           @Param("noneStatus") LoanExtensionStatus noneStatus);

    @Query("""
            SELECT COUNT(l)
            FROM LoanEntity l
            JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
            WHERE u.school.id = :schoolId
            """)
    long countActiveLoansForSchool(@Param("schoolId") Long schoolId);

    @Query("""
            SELECT COUNT(l)
            FROM LoanEntity l
            JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
            WHERE u.school.id = :schoolId AND l.dueDate < :today
            """)
    long countOverdueLoansForSchool(@Param("schoolId") Long schoolId, @Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(l)
            FROM LoanEntity l
            JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
            WHERE u.school.id = :schoolId AND l.extensionStatus = edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus.PENDING
            """)
    long countPendingExtensionsForSchool(@Param("schoolId") Long schoolId);
}