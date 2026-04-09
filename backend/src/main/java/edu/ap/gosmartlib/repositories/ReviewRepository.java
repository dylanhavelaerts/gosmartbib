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

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ReviewEntity r WHERE r.id = :reviewId AND LOCATE(CONCAT(',', :uid, ','), CONCAT(',', COALESCE(r.flaggedByUids, ''), ',')) > 0")
    boolean existsFlagByReviewIdAndUid(@Param("reviewId") Long reviewId, @Param("uid") String uid);

    @Modifying
    @Query("UPDATE ReviewEntity r SET r.flagCount = r.flagCount + 1 WHERE r.id = :id")
    void incrementFlagCount(@Param("id") Long id);
}
