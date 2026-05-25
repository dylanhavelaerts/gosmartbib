package edu.ap.gosmartlib.repositories;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolClassEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
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