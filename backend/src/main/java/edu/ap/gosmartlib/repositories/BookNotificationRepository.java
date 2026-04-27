package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.BookNotificationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookNotificationRepository extends JpaRepository<BookNotificationEntity, Long> {

    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_IdAndBook_Id(Long userId, Long bookId);

    @EntityGraph(attributePaths = {"user", "user.school"})
    List<BookNotificationEntity> findAllByBook_IdAndUser_School_Id(Long bookId, Long schoolId);
}
