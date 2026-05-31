package edu.ap.gosmartlib.repositories.school;

import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, Long> {
    Optional<SchoolClassEntity> findBySmartschoolGroupId(String smartschoolGroupId);

    /**
     * Zoekt een bestaande klas binnen dezelfde school op basis van de zichtbare
     * klasnaam.
     *
     * <p>
     * Dit wordt gebruikt om dubbele klasrecords te vermijden wanneer verschillende
     * externe bronnen, zoals Smartschool en OneRoster, dezelfde klas met een andere
     * externe ID aanleveren.
     * </p>
     *
     * @param schoolId interne school-ID
     * @param name     zichtbare klasnaam
     * @return de eerste klas met dezelfde naam binnen de school, indien aanwezig
     */
    Optional<SchoolClassEntity> findFirstBySchool_IdAndNameIgnoreCase(Long schoolId, String name);

    List<SchoolClassEntity> findAllBySchool_IdOrderByNameAsc(Long schoolId);

    List<SchoolClassEntity> findTop20BySchool_IdAndNameContainingIgnoreCaseOrderByNameAsc(
            Long schoolId,
            String name);
}