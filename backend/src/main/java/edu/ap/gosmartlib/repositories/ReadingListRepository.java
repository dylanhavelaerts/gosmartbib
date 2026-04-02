package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.ReadingListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingListRepository extends JpaRepository<ReadingListEntity, Long> {
}