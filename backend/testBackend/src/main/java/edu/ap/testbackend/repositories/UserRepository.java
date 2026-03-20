package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findBySmartschoolUid(String smartschoolUid);
    List<UserEntity> findAllByActiveIsFalseAndScheduledDeletionAtBefore(LocalDateTime cutoff);
}