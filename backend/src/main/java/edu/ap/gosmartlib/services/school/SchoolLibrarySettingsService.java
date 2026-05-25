package edu.ap.gosmartlib.services.school;

import edu.ap.gosmartlib.dto.school.SchoolLibrarySettingsDTO;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolLibrarySettingsEntity;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolLibrarySettingsRepository;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class SchoolLibrarySettingsService {

    private final SchoolLibrarySettingsRepository librarySettingsRepository;
    private final SchoolRepository schoolRepository;

    public SchoolLibrarySettingsDTO getSettings(Long schoolId) {
        return librarySettingsRepository.findBySchool_Id(schoolId)
                .map(this::toDTO)
                .orElseGet(() -> new SchoolLibrarySettingsDTO(schoolId, false));
    }

    public SchoolLibrarySettingsDTO saveSettings(Long schoolId, SchoolLibrarySettingsDTO request) {
        SchoolLibrarySettingsEntity settings = librarySettingsRepository.findBySchool_Id(schoolId)
                .orElseGet(() -> {
                    SchoolEntity school = schoolRepository.findById(schoolId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden."));

                    return new SchoolLibrarySettingsEntity(school);
                });

        settings.setBarcodesEnabled(request.barcodesEnabled());

        return toDTO(librarySettingsRepository.save(settings));
    }

    private SchoolLibrarySettingsDTO toDTO(SchoolLibrarySettingsEntity entity) {
        return new SchoolLibrarySettingsDTO(
                entity.getSchool().getId(),
                entity.isBarcodesEnabled()
        );
    }
}
