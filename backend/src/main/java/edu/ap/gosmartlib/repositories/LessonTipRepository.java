package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.LessonTipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonTipRepository extends JpaRepository<LessonTipEntity, Long> {
    List<LessonTipEntity> findByBook_IdOrderByCreatedDateDesc(Long bookId);
}
