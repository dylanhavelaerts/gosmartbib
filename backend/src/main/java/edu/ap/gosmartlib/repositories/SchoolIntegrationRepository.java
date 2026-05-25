package edu.ap.gosmartlib.repositories;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolIntegrationRepository extends JpaRepository<SchoolIntegrationEntity, Long> {

    @EntityGraph(attributePaths = "school")
    Optional<SchoolIntegrationEntity> findBySchool_Id(Long schoolId);

    @EntityGraph(attributePaths = "school")
    List<SchoolIntegrationEntity> findAllByOnerosterEnabledTrue();
}