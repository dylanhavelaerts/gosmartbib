package edu.ap.gosmartlib.repositories.LoanRepositories;

import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanHistoryRepository extends JpaRepository<LoanHistoryEntity, Long> {
    // Haalt de uitleengeschiedenis op voor een specifieke gebruiker
    List<LoanHistoryEntity> findBySmartschoolUserIdOrderByReturnDateDesc(String smartschoolUserId);


    @Query("""
            select h
            from LoanHistoryEntity h
            join UserEntity u on u.smartschoolUid = h.smartschoolUserId
            where u.school.id = :schoolId
            order by h.returnDate desc
            """)
    List<LoanHistoryEntity> findAllBySchoolIdOrderByReturnDateDesc(@Param("schoolId") Long schoolId);

    @Query("""
            select h
            from LoanHistoryEntity h
            join UserEntity u on u.smartschoolUid = h.smartschoolUserId
            join u.classes cls
            where u.school.id = :schoolId
            and cls.id = :classId
            order by h.returnDate desc
            """)
    List<LoanHistoryEntity> findAllBySchoolIdAndClassIdOrderByReturnDateDesc(
            @Param("schoolId") Long schoolId,
            @Param("classId") Long classId);
}