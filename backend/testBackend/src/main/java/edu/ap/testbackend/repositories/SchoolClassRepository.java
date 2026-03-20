package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.SchoolClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, Long> {
    Optional<SchoolClassEntity> findBySmartschoolGroupId(String smartschoolGroupId);
}