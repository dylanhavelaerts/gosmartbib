package edu.ap.gosmartlib.services.school;

import edu.ap.gosmartlib.dto.school.HomepageSettingsDTO;
import edu.ap.gosmartlib.entities.HomepageSettingsEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.repositories.HomepageSettingsRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class HomepageSettingsService {

    private final HomepageSettingsRepository homepageSettingsRepository;
    private final SchoolRepository schoolRepository;

    public HomepageSettingsDTO getSettings(Long schoolId) {
        return homepageSettingsRepository.findBySchool_Id(schoolId)
                .map(this::toDTO)
                // Als er nog geen instellingen zijn, geef de default (alles true) terug
                .orElseGet(() -> new HomepageSettingsDTO(schoolId, true, true, true, true)); 
    }

    public HomepageSettingsDTO saveSettings(Long schoolId, HomepageSettingsDTO request) {
        HomepageSettingsEntity settings = homepageSettingsRepository.findBySchool_Id(schoolId)
                .orElseGet(() -> {
                    SchoolEntity school = schoolRepository.findById(schoolId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden."));
                    return new HomepageSettingsEntity(school);
                });

        settings.setShowSpotlight(request.showSpotlight());
        settings.setShowNewInLibrary(request.showNewInLibrary());
        settings.setShowReadingLists(request.showReadingLists());
        settings.setShowUrgentLoans(request.showUrgentLoans());

        return toDTO(homepageSettingsRepository.save(settings));
    }

    private HomepageSettingsDTO toDTO(HomepageSettingsEntity entity) {
        return new HomepageSettingsDTO(
                entity.getSchool().getId(),
                entity.isShowSpotlight(),
                entity.isShowNewInLibrary(),
                entity.isShowReadingLists(),
                entity.isShowUrgentLoans()
        );
    }
}