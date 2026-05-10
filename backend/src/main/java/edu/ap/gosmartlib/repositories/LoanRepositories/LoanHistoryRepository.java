package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    GROUP BY b
    ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> findMostPopularBooks(Long schoolId, Pageable pageable);

    @Query("""
    SELECT c, COUNT(lh)
    FROM LoanHistoryEntity lh
    JOIN BookEntity b ON b.isbn = lh.isbn
    JOIN b.categories c
    JOIN UserEntity u ON u.smartschoolUid = lh.smartschoolUserId
    WHERE u.school.id = :schoolId
    GROUP BY c
    ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> findMostReadGenres(Long schoolId, Pageable pageable);

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
}