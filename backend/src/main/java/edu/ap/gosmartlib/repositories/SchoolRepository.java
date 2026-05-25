package edu.ap.gosmartlib.repositories;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<SchoolEntity, Long> {
    Optional<SchoolEntity> findByDomain(String domain);
    boolean existsByDomain(String domain);
    List<SchoolEntity> findAllByOrderByAdminApprovedAscNameAsc();
}
