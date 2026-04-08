package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.ReadingListEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReadingListRepository extends JpaRepository<ReadingListEntity, Long> {
    
    @EntityGraph(attributePaths = {"books"})
    List<ReadingListEntity> findByCreatorId(Long creatorId);

    @EntityGraph(attributePaths = {"books"})
    Optional<ReadingListEntity> findById(Long id);
}