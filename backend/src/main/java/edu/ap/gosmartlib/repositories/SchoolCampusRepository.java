package edu.ap.gosmartlib.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolCampusEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolCampusEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import java.util.List;
import java.util.Optional;

public interface SchoolCampusRepository extends JpaRepository<SchoolCampusEntity, Long> {

    List<SchoolCampusEntity> findBySchool_IdOrderByNameAsc(Long schoolId);

    Optional<SchoolCampusEntity> findByIdAndSchool_Id(Long campusId, Long schoolId);

    boolean existsBySchool_IdAndNameIgnoreCase(Long schoolId, String name);
}