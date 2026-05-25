package edu.ap.gosmartlib.repositories.loanRepositories;

import edu.ap.gosmartlib.entities.loanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.loanEntities.LoanExtensionStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT l.extensionStatus, COUNT(l)
            FROM LoanEntity l
            JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
            WHERE u.school.id = :schoolId AND l.extensionStatus <> :noneStatus
            AND (:className IS NULL OR EXISTS (
                SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
            AND (:grade IS NULL OR EXISTS (
                SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.grade = :grade
            ))
            GROUP BY l.extensionStatus
            """)
    List<Object[]> countExtensionsByStatus(@Param("schoolId") Long schoolId,
                                           @Param("noneStatus") LoanExtensionStatus noneStatus,
                                           @Param("className") String className,
                                           @Param("grade") String grade);

    @Query("""
        SELECT COUNT(l)
        FROM LoanEntity l
        JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
        WHERE u.school.id = :schoolId
        AND (:className IS NULL OR EXISTS (
            SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
        ))
        AND (:grade IS NULL OR EXISTS (
            SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.grade = :grade
        ))
        """)
    long countActiveLoansForSchool(@Param("schoolId") Long schoolId, @Param("className") String className, @Param("grade") String grade);

        @Query("""
            SELECT COUNT(l)
            FROM LoanEntity l
            JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
            WHERE u.school.id = :schoolId AND l.dueDate < :today
            AND (:className IS NULL OR EXISTS (
                SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className))
            AND (:grade IS NULL OR EXISTS (
                SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.grade = :grade))
            """)
        long countOverdueLoansForSchool(@Param("schoolId") Long schoolId, @Param("today") LocalDate today, @Param("className") String className, @Param("grade") String grade);

        @Query("""
            SELECT COUNT(l)
            FROM LoanEntity l
            JOIN UserEntity u ON u.smartschoolUid = l.smartschoolUserId
            WHERE u.school.id = :schoolId AND l.extensionStatus = edu.ap.gosmartlib.entities.loanEntities.LoanExtensionStatus.PENDING
            AND (:className IS NULL OR EXISTS (
                SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className))
            AND (:grade IS NULL OR EXISTS (
                SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.grade = :grade))
            """)
        long countPendingExtensionsForSchool(@Param("schoolId") Long schoolId, @Param("className") String className, @Param("grade") String grade);


        @Query(value = """
            select loan from LoanEntity loan
            join UserEntity user on user.smartschoolUid = loan.smartschoolUserId
            where user.school.id = :schoolId
            order by loan.dueDate asc
            """,
            countQuery = """
                    select count(loan) from LoanEntity loan
                    join UserEntity user on user.smartschoolUid = loan.smartschoolUserId
                    where user.school.id = :schoolId
                    """)
        Page<LoanEntity> findAllActiveBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

        @Query(value = """
            select distinct loan from LoanEntity loan
            join UserEntity user on user.smartschoolUid = loan.smartschoolUserId
            join user.classes cls
            where user.school.id = :schoolId and cls.id = :classId
            order by loan.dueDate asc
            """,
            countQuery = """
                    select count(distinct loan) from LoanEntity loan
                    join UserEntity user on user.smartschoolUid = loan.smartschoolUserId
                    join user.classes cls
                    where user.school.id = :schoolId and cls.id = :classId
                    """)
        Page<LoanEntity> findAllActiveBySchoolIdAndClassId(
            @Param("schoolId") Long schoolId, @Param("classId") Long classId, Pageable pageable);

        @Modifying
        @Query("UPDATE LoanEntity l SET l.smartschoolUserId = null WHERE l.smartschoolUserId = :uid")
        void anonymizeBySmartschoolUid(@Param("uid") String uid);

        @Modifying
        @Query("UPDATE LoanEntity l SET l.extensionDecidedBySmartschoolUserId = null WHERE l.extensionDecidedBySmartschoolUserId = :uid")
        void anonymizeExtensionDeciderBySmartschoolUid(@Param("uid") String uid);

}