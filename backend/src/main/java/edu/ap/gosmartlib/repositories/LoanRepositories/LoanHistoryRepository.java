package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanHistoryRepository extends JpaRepository<LoanHistoryEntity, Long> {
    // Haalt de uitleengeschiedenis op voor een specifieke gebruiker
    List<LoanHistoryEntity> findBySmartschoolUserIdOrderByReturnDateDesc(String smartschoolUserId);

    @Query("""
    SELECT b, COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN BookEntity b ON b.isbn = lh.isbn
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId
    AND (:className IS NULL OR EXISTS (
        SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
    GROUP BY b
    ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> findMostPopularBooks(Long schoolId, Pageable pageable,String className);

    @Query("""
    SELECT c, COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN BookEntity b ON b.isbn = lh.isbn
    JOIN b.categories c
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId
    AND (:className IS NULL OR EXISTS (
        SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
    GROUP BY c
    ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> findMostReadGenres(Long schoolId, Pageable pageable,String className);

    @Query("""
    SELECT sc.name, sc.grade, sc.schoolYear, COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    JOIN u.classes sc
    WHERE u.school.id = :schoolId
    GROUP BY sc.name, sc.grade, sc.schoolYear
    ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> findMostReadingClasses(Long schoolId);

    @Query("""
    SELECT COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId AND lh.returnDate <= lh.dueDate
    AND (:className IS NULL OR EXISTS (
            SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
        ))
    """)
    long countOnTimeReturns(Long schoolId,String className);

    @Query("""
    SELECT COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId AND lh.returnDate > lh.dueDate
    AND (:className IS NULL OR EXISTS (
        SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
    """)
    long countLateReturns(Long schoolId,String className);

    @Query("""
    SELECT FUNCTION('DATEDIFF', lh.returnDate, lh.loanDate), COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId
    AND (:className IS NULL OR EXISTS (
        SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
    GROUP BY FUNCTION('DATEDIFF', lh.returnDate, lh.loanDate)
    ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> findLoanDurationDistribution(Long schoolId,String className);

    @Query("""
    SELECT lh.smartschoolUserId, COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId
    AND (:className IS NULL OR EXISTS (
        SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
    GROUP BY lh.smartschoolUserId
    ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> findTopReaders(Long schoolId, Pageable pageable,String className);

    @Query("""
    SELECT FUNCTION('YEAR', lh.loanDate), FUNCTION('MONTH', lh.loanDate), COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId
    AND (:className IS NULL OR EXISTS (
        SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
    GROUP BY FUNCTION('YEAR', lh.loanDate), FUNCTION('MONTH', lh.loanDate)
    ORDER BY FUNCTION('YEAR', lh.loanDate) ASC, FUNCTION('MONTH', lh.loanDate) ASC
    """)
    List<Object[]> findLoansPerMonth(Long schoolId,String className);

    @Query("""
    SELECT b, COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN BookEntity b ON b.isbn = lh.isbn
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId
    AND (:className IS NULL OR EXISTS (
        SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className
            ))
    GROUP BY b
    ORDER BY COUNT(lh) ASC
    """)
    List<Object[]> findLeastPopularBooks(Long schoolId, Pageable pageable,String className);


    @Query(value = """
    select h from LoanHistoryEntity h
    join UserEntity u on u.smartschoolUid = h.smartschoolUserId
    where u.school.id = :schoolId
    order by h.returnDate desc
    """,
            countQuery = """
    select count(h) from LoanHistoryEntity h
    join UserEntity u on u.smartschoolUid = h.smartschoolUserId
    where u.school.id = :schoolId
    """)
    Page<LoanHistoryEntity> findAllBySchoolIdOrderByReturnDateDesc(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query(value = """
    select distinct h from LoanHistoryEntity h
    join UserEntity u on u.smartschoolUid = h.smartschoolUserId
    join u.classes cls
    where u.school.id = :schoolId and cls.id = :classId
    order by h.returnDate desc
    """,
            countQuery = """
    select count(distinct h) from LoanHistoryEntity h
    join UserEntity u on u.smartschoolUid = h.smartschoolUserId
    join u.classes cls
    where u.school.id = :schoolId and cls.id = :classId
    """)
    Page<LoanHistoryEntity> findAllBySchoolIdAndClassIdOrderByReturnDateDesc(
            @Param("schoolId") Long schoolId, @Param("classId") Long classId, Pageable pageable);

        @Modifying
        @Query("UPDATE LoanHistoryEntity l SET l.smartschoolUserId = null WHERE l.smartschoolUserId = :uid")
        void anonymizeBySmartschoolUid(@Param("uid") String uid);

}