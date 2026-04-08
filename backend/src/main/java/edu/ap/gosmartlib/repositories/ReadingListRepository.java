package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.util.ReadingListType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingListRepository extends JpaRepository<ReadingListEntity, Long> {
    List<ReadingListEntity> findAllByCreator_IdOrderByIdDesc(Long creatorId);
    List<ReadingListEntity> findAllByListTypeOrderByIdDesc(ReadingListType listType);
    Optional<ReadingListEntity> findByIdAndCreator_Id(Long id, Long creatorId);
//    zonder dit triggert een sql query per boek -> performanter
    @Query("SELECT rl FROM ReadingListEntity rl LEFT JOIN FETCH rl.books WHERE rl.id = :id")
    Optional<ReadingListEntity> findByIdWithBooks(@Param("id") Long id);
}