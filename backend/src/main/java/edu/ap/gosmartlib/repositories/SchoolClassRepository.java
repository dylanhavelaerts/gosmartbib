package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.schoolEntities.SchoolClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, Long> {
    Optional<SchoolClassEntity> findBySmartschoolGroupId(String smartschoolGroupId);

    List<SchoolClassEntity> findAllBySchool_IdOrderByNameAsc(Long schoolId);

    List<SchoolClassEntity> findTop20BySchool_IdAndNameContainingIgnoreCaseOrderByNameAsc(
            Long schoolId,
            String name);
}