package edu.ap.gosmartlib.services.schoolIntegration.schoolCampus;

import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.CreateSchoolCampusRequest;
import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.SchoolCampusDTO;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolCampusEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolCampusRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
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

    @Transactional(readOnly = true)
    public List<SchoolCampusDTO> getCampusesForBibbeheerder(String actorUid, Long schoolId) {
        UserEntity actor = getCurrentBibbeheerder(actorUid);
        assertAdminBelongsToSchool(actor, schoolId);

        return schoolCampusRepository.findBySchool_IdOrderByNameAsc(schoolId)
                .stream()
                .map(SchoolCampusDTO::from)
                .toList();
    }

    @Transactional
    public SchoolCampusDTO createCampusForBibbeheerder(
            String actorUid,
            Long schoolId,
            CreateSchoolCampusRequest request) {
        UserEntity actor = getCurrentBibbeheerder(actorUid);
        assertAdminBelongsToSchool(actor, schoolId);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body ontbreekt");
        }

        String campusName = normalizeCampusName(request.name());

        if (campusName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campusnaam is verplicht");
        }

        if (schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(schoolId, campusName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campus bestaat al voor deze school");
        }

        SchoolCampusEntity campus = new SchoolCampusEntity();
        campus.setSchool(actor.getSchool());
        campus.setName(campusName);

        SchoolCampusEntity savedCampus = schoolCampusRepository.save(campus);

        return SchoolCampusDTO.from(savedCampus);
    }

    @Transactional
    public void deleteCampusForBibbeheerder(String actorUid, Long schoolId, Long campusId) {
        UserEntity actor = getCurrentBibbeheerder(actorUid);
        assertAdminBelongsToSchool(actor, schoolId);

        SchoolCampusEntity campus = schoolCampusRepository.findByIdAndSchool_Id(campusId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus niet gevonden"));

        schoolCampusRepository.delete(campus);
    }

    @Transactional(readOnly = true)
    public List<SchoolCampusDTO> getCampusesForPlatformAdmin(Long schoolId) {
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        return schoolCampusRepository.findBySchool_IdOrderByNameAsc(schoolId)
                .stream().map(SchoolCampusDTO::from).toList();
    }

    @Transactional
    public SchoolCampusDTO createCampusForPlatformAdmin(Long schoolId, CreateSchoolCampusRequest request) {
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body ontbreekt");

        String campusName = normalizeCampusName(request.name());
        if (campusName.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campusnaam is verplicht");
        if (schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(schoolId, campusName))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campus bestaat al voor deze school");

        SchoolEntity school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"));
        SchoolCampusEntity campus = new SchoolCampusEntity();
        campus.setSchool(school);
        campus.setName(campusName);
        return SchoolCampusDTO.from(schoolCampusRepository.save(campus));
    }

    @Transactional
    public void deleteCampusForPlatformAdmin(Long schoolId, Long campusId) {
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        SchoolCampusEntity campus = schoolCampusRepository.findByIdAndSchool_Id(campusId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus niet gevonden"));
        schoolCampusRepository.delete(campus);
    }


    private UserEntity getCurrentBibbeheerder(String actorUid) {
        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingelogde gebruiker niet gevonden"));
        if (actor.getRole() != UserRoles.BIBLIOTHEEKBEHEERDER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang");
        return actor;
    }

    private void assertAdminBelongsToSchool(UserEntity actor, Long schoolId) {
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "School ontbreekt");
        if (!actor.getSchool().getId().equals(schoolId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang tot deze school");
    }

    private String normalizeCampusName(String campusName) {
        if (campusName == null) {
            return "";
        }

        return campusName.trim().replaceAll("\\s+", " ");
    }
}