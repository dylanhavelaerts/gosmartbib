package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.BookNotificationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookNotificationRepository extends JpaRepository<BookNotificationEntity, Long> {

    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_IdAndBook_Id(Long userId, Long bookId);

    @EntityGraph(attributePaths = {"user", "user.school"})
    List<BookNotificationEntity> findAllByBook_IdAndUser_School_Id(Long bookId, Long schoolId);

    @Query("""
            SELECT bn.book, COUNT(bn)
            FROM BookNotificationEntity bn
            JOIN bn.user u
            WHERE u.school.id = :schoolId
            GROUP BY bn.book
            ORDER BY COUNT(bn) DESC
            """)
    List<Object[]> findMostWantedBooks(@Param("schoolId") Long schoolId, Pageable pageable);

    void deleteByUser(UserEntity user);
}
