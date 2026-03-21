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
     * Laad de hele user met alle details
     */
    @EntityGraph(attributePaths = { "school", "classes" })
    Optional<UserEntity> findDetailedBySmartschoolUid(String smartschoolUid);

    List<UserEntity> findAllByActiveIsFalseAndScheduledDeletionAtBefore(LocalDateTime cutoff);
}