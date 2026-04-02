package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.ReviewEntity;
import edu.ap.gosmartlib.util.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByBook_Isbn(String isbn);

    List<ReviewEntity> findByUser_SmartschoolUid(String smartschoolUid);

    List<ReviewEntity> findByStatus(ReviewStatus status);

    boolean existsByUser_SmartschoolUidAndBook_Isbn(String smartschoolUid, String isbn);

    @Modifying
    @Query("UPDATE ReviewEntity r SET r.flagCount = r.flagCount + 1 WHERE r.id = :id")
    void incrementFlagCount(@Param("id") Long id);
}
