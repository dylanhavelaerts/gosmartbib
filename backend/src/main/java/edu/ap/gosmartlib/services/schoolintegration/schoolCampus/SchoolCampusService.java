package edu.ap.gosmartlib.services.schoolintegration.schoolCampus;

import edu.ap.gosmartlib.dto.schoolintegration.schoolCampus.CreateSchoolCampusRequest;
import edu.ap.gosmartlib.dto.schoolintegration.schoolCampus.SchoolCampusDTO;
import edu.ap.gosmartlib.entities.school.SchoolCampusEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.book.BookInventoryRepository;
import edu.ap.gosmartlib.repositories.school.SchoolCampusRepository;
import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolCampusService {

    private final SchoolCampusRepository schoolCampusRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final BookInventoryRepository bookInventoryRepository;

    @Transactional(readOnly = true)
    public List<SchoolCampusDTO> getCampusesForLibrarian(String actorUid, Long schoolId) {
        UserEntity actor = getCurrentLibrarian(actorUid);
        assertAdminBelongsToSchool(actor, schoolId);

        return schoolCampusRepository.findBySchool_IdOrderByNameAsc(schoolId)
                .stream()
                .map(SchoolCampusDTO::from)
                .toList();
    }

    @Transactional
    public SchoolCampusDTO createCampusForLibrarian(
            String actorUid,
            Long schoolId,
            CreateSchoolCampusRequest request) {
        UserEntity actor = getCurrentLibrarian(actorUid);
        assertAdminBelongsToSchool(actor, schoolId);

        return createCampus(actor.getSchool(), request);
    }

    @Transactional
    public SchoolCampusDTO updateCampusForLibrarian(
            String actorUid,
            Long schoolId,
            Long campusId,
            CreateSchoolCampusRequest request) {
        UserEntity actor = getCurrentLibrarian(actorUid);
        assertAdminBelongsToSchool(actor, schoolId);

        return updateCampus(schoolId, campusId, request);
    }

    @Transactional
    public void deleteCampusForLibrarian(String actorUid, Long schoolId, Long campusId) {
        UserEntity actor = getCurrentLibrarian(actorUid);
        assertAdminBelongsToSchool(actor, schoolId);

        deleteCampus(schoolId, campusId);
    }

    @Transactional(readOnly = true)
    public List<SchoolCampusDTO> getCampusesForPlatformAdmin(Long schoolId) {
        if (schoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        }

        return schoolCampusRepository.findBySchool_IdOrderByNameAsc(schoolId)
                .stream()
                .map(SchoolCampusDTO::from)
                .toList();
    }

    @Transactional
    public SchoolCampusDTO createCampusForPlatformAdmin(Long schoolId, CreateSchoolCampusRequest request) {
        if (schoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        }

        SchoolEntity school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"));

        return createCampus(school, request);
    }

    @Transactional
    public void deleteCampusForPlatformAdmin(Long schoolId, Long campusId) {
        if (schoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        }

        deleteCampus(schoolId, campusId);
    }

    private UserEntity getCurrentLibrarian(String actorUid) {
        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingelogde gebruiker niet gevonden"));
        if (actor.getRole() != UserRoles.LIBRARIAN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang");
        return actor;
    }

    private void assertAdminBelongsToSchool(UserEntity actor, Long schoolId) {
        if (schoolId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "School ontbreekt");
        if (!actor.getSchool().getId().equals(schoolId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang tot deze school");
    }

    private String normalizeCampusName(String campusName) {
        if (campusName == null) {
            return "";
        }

        return campusName.trim().replaceAll("\\s+", " ");
    }

    @Transactional
    public SchoolCampusDTO updateCampusForPlatformAdmin(Long schoolId, Long campusId,
            CreateSchoolCampusRequest request) {
        if (schoolId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        return updateCampus(schoolId, campusId, request);
    }

    private SchoolCampusDTO createCampus(SchoolEntity school, CreateSchoolCampusRequest request) {
        String campusName = validateCampusRequest(request);

        if (schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(school.getId(), campusName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campus bestaat al voor deze school");
        }

        SchoolCampusEntity campus = new SchoolCampusEntity();
        campus.setSchool(school);
        campus.setName(campusName);

        return SchoolCampusDTO.from(schoolCampusRepository.save(campus));
    }

    private SchoolCampusDTO updateCampus(Long schoolId, Long campusId, CreateSchoolCampusRequest request) {
        String newCampusName = validateCampusRequest(request);

        SchoolCampusEntity campus = schoolCampusRepository.findByIdAndSchool_Id(campusId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus niet gevonden"));

        String oldCampusName = normalizeCampusName(campus.getName());

        if (oldCampusName.equalsIgnoreCase(newCampusName)) {
            campus.setName(newCampusName);
            return SchoolCampusDTO.from(schoolCampusRepository.save(campus));
        }

        if (schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(schoolId, newCampusName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campus bestaat al voor deze school");
        }

        campus.setName(newCampusName);
        SchoolCampusEntity savedCampus = schoolCampusRepository.save(campus);

        bookInventoryRepository.renameCampusForSchool(schoolId, oldCampusName, newCampusName);

        return SchoolCampusDTO.from(savedCampus);
    }

    private String validateCampusRequest(CreateSchoolCampusRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body ontbreekt");
        }

        String campusName = normalizeCampusName(request.name());

        if (campusName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campusnaam is verplicht");
        }

        return campusName;
    }

    private void deleteCampus(Long schoolId, Long campusId) {
        SchoolCampusEntity campus = schoolCampusRepository.findByIdAndSchool_Id(campusId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus niet gevonden"));

        if (schoolCampusRepository.countBySchool_Id(schoolId) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "De laatste campus van een school kan niet verwijderd worden");
        }

        if (bookInventoryRepository.existsBySchool_IdAndCampusIgnoreCase(schoolId, campus.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Deze campus wordt nog gebruikt door boekinventaris en kan niet verwijderd worden");
        }

        schoolCampusRepository.delete(campus);
    }
}