package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.UserEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findBySmartschoolUid(String smartschoolUid);

    /**
     * Laad de hele user met alle nodige details
     */
    @EntityGraph(attributePaths = { "school", "classes" })
    Optional<UserEntity> findDetailedBySmartschoolUid(String smartschoolUid);

    /**
     * Vindt alle users van een zekere school
     */
    @EntityGraph(attributePaths = { "school", "classes" })
    List<UserEntity> findAllBySchool_IdAndActiveIsTrueOrderBySmartschoolUidAsc(Long schoolId);

    /**
     * Vindt één specifieke user van een zekere school (scope zo klein mogelijk
     * houden)
     */
    @EntityGraph(attributePaths = { "school", "classes" })
    Optional<UserEntity> findByIdAndSchool_Id(Long id, Long schoolId);

    List<UserEntity> findAllByActiveIsFalseAndScheduledDeletionAtBefore(LocalDateTime cutoff);
}