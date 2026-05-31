package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.repositories.school.SchoolClassRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SchoolClassHelper {

    private final SchoolClassRepository schoolClassRepository;

    /**
     * Zoekt of maakt een lokale klas voor een Smartschoolgroep.
     *
     * <p>
     * Eerst wordt gezocht op de externe groupId. Als die niet bestaat, wordt ook
     * gezocht op school en klasnaam. Zo wordt vermeden dat dezelfde klas dubbel
     * wordt aangemaakt wanneer dezelfde zichtbare klas later via een andere bron,
     * zoals OneRoster, met een andere externe ID binnenkomt.
     * </p>
     *
     * @param school     school waartoe de klas behoort
     * @param groupId    externe Smartschool groupID
     * @param name       zichtbare klasnaam
     * @param schoolYear huidig schooljaar
     * @param grade      afgeleide graad of jaargroep
     * @return bestaande of nieuw aangemaakte klas
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SchoolClassEntity findOrCreate(
            SchoolEntity school, String groupId, String name, String schoolYear, String grade) {
        try {
            Optional<SchoolClassEntity> existingByGroupId = groupId == null || groupId.isBlank()
                    ? Optional.empty()
                    : schoolClassRepository.findBySmartschoolGroupId(groupId);

            if (existingByGroupId.isPresent()) {
                return existingByGroupId.get();
            }

            Optional<SchoolClassEntity> existingByName = school != null
                    && school.getId() != null
                    && name != null
                    && !name.isBlank()
                            ? schoolClassRepository.findFirstBySchool_IdAndNameIgnoreCase(school.getId(), name.trim())
                            : Optional.empty();

            if (existingByName.isPresent()) {
                SchoolClassEntity schoolClass = existingByName.get();

                if (groupId != null && !groupId.isBlank()) {
                    schoolClass.setSmartschoolGroupId(groupId);
                }
                if (schoolYear != null && !schoolYear.isBlank()) {
                    schoolClass.setSchoolYear(schoolYear);
                }
                if (grade != null && !grade.isBlank()) {
                    schoolClass.setGrade(grade);
                }

                return schoolClassRepository.save(schoolClass);
            }

            return schoolClassRepository.save(
                    new SchoolClassEntity(school, groupId, name, schoolYear, grade));
        } catch (DataIntegrityViolationException e) {
            if (groupId != null && !groupId.isBlank()) {
                return schoolClassRepository.findBySmartschoolGroupId(groupId)
                        .orElseThrow(() -> e);
            }

            throw e;
        }
    }
}
