package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.entities.schoolEntities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SchoolClassHelper {

    private final SchoolClassRepository schoolClassRepository;

    // Elke call van deze methode zal een nieuwe transactie starten, zodat we kunnen
    // omgaan met gelijktijdige aanroepen die dezelfde klas proberen aan te maken zonder dat er een unieke index fout optreedt
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SchoolClassEntity findOrCreate(
            SchoolEntity school, String groupId, String name, String schoolYear, String grade) {
        try {
            return schoolClassRepository.findBySmartschoolGroupId(groupId)
                    .orElseGet(() -> schoolClassRepository.save(
                            new SchoolClassEntity(school, groupId, name, schoolYear, grade)));
        } catch (DataIntegrityViolationException e) {
            return schoolClassRepository.findBySmartschoolGroupId(groupId)
                    .orElseThrow(() -> e);
        }
    }
}
