package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.HomepageSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HomepageSettingsRepository extends JpaRepository<HomepageSettingsEntity, Long> {
    Optional<HomepageSettingsEntity> findBySchool_Id(Long schoolId);
}