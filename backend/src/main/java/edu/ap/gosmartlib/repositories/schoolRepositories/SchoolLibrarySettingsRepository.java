package edu.ap.gosmartlib.repositories.schoolRepositories;

import edu.ap.gosmartlib.entities.schoolEntities.SchoolLibrarySettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolLibrarySettingsRepository extends JpaRepository<SchoolLibrarySettingsEntity, Long> {
    Optional<SchoolLibrarySettingsEntity> findBySchool_Id(Long schoolId);
}

