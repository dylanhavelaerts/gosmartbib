package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}