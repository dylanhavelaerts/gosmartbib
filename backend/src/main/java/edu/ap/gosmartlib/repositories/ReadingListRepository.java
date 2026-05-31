package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.util.ReadingListType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository voor het opvragen en beheren van leeslijsten.
 */
public interface ReadingListRepository extends JpaRepository<ReadingListEntity, Long> {
    List<ReadingListEntity> findAllByCreator_IdOrderByIdDesc(Long creatorId);

    List<ReadingListEntity> findAllByListTypeOrderByIdDesc(ReadingListType listType);

    Optional<ReadingListEntity> findByIdAndCreator_Id(Long id, Long creatorId);

    // zonder dit triggert een sql query per boek -> performanter
    @Query("SELECT DISTINCT rl FROM ReadingListEntity rl LEFT JOIN FETCH rl.books WHERE rl.id = :id")
    Optional<ReadingListEntity> findByIdWithBooks(@Param("id") Long id);

    @Query("SELECT DISTINCT rl FROM ReadingListEntity rl LEFT JOIN FETCH rl.books WHERE rl.publicUid = :publicUid")
    Optional<ReadingListEntity> findByPublicUidWithBooks(@Param("publicUid") String publicUid);

    @Modifying
    @Query("DELETE FROM ReadingListEntity r WHERE r.creator = :user")
    void deleteByCreator(@Param("user") UserEntity user);
}