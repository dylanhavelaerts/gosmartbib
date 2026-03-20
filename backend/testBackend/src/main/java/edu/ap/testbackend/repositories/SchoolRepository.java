package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<SchoolEntity, Long> {
    Optional<SchoolEntity> findByDomain(String domain);
}
