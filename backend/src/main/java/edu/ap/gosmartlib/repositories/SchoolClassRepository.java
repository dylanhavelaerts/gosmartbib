package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.SchoolClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, Long> {
    Optional<SchoolClassEntity> findBySmartschoolGroupId(String smartschoolGroupId);
}