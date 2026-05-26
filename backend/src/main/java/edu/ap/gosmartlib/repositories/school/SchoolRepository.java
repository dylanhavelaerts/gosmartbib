package edu.ap.gosmartlib.repositories.school;

import edu.ap.gosmartlib.entities.school.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<SchoolEntity, Long> {
    Optional<SchoolEntity> findByDomain(String domain);
    boolean existsByDomain(String domain);
    List<SchoolEntity> findAllByOrderByAdminApprovedAscNameAsc();
}
