package edu.ap.gosmartlib.repositories.school;

import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, Long> {
    Optional<SchoolClassEntity> findBySmartschoolGroupId(String smartschoolGroupId);

    Optional<SchoolClassEntity> findFirstBySchool_IdAndNameIgnoreCase(Long schoolId, String name);

    List<SchoolClassEntity> findAllBySchool_IdOrderByNameAsc(Long schoolId);

    List<SchoolClassEntity> findTop20BySchool_IdAndNameContainingIgnoreCaseOrderByNameAsc(
            Long schoolId,
            String name);
}