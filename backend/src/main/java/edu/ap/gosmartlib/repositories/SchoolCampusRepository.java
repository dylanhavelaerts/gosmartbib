package edu.ap.gosmartlib.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.ap.gosmartlib.entities.school.SchoolCampusEntity;
import java.util.List;
import java.util.Optional;

public interface SchoolCampusRepository extends JpaRepository<SchoolCampusEntity, Long> {

    List<SchoolCampusEntity> findBySchool_IdOrderByNameAsc(Long schoolId);

    Optional<SchoolCampusEntity> findByIdAndSchool_Id(Long campusId, Long schoolId);

    boolean existsBySchool_IdAndNameIgnoreCase(Long schoolId, String name);
}