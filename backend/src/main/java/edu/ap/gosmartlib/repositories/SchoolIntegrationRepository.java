package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolIntegrationRepository extends JpaRepository<SchoolIntegrationEntity, Long> {

    @EntityGraph(attributePaths = "school")
    Optional<SchoolIntegrationEntity> findBySchool_Id(Long schoolId);
}