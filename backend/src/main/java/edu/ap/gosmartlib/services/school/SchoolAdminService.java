package edu.ap.gosmartlib.services.school;

import edu.ap.gosmartlib.dto.school.ApproveSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolResult;
import edu.ap.gosmartlib.dto.school.SchoolDTO;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolAdminService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserDeletionService userDeletionService;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolIntegrationRepository schoolIntegrationRepository;

    public List<SchoolDTO> listAllSchools() {
        return schoolRepository.findAllByOrderByAdminApprovedAscNameAsc()
                .stream()
                .map(SchoolDTO::from)
                .toList();
    }

    @Transactional
    public CreateSchoolResult createSchool(CreateSchoolRequest request) {
        String name = request.name() != null ? request.name().trim() : "";
        String domain = request.domain() != null ? request.domain().trim() : "";

        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schoolnaam is verplicht");
        if (domain.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schooldomein is verplicht");

        var existing = schoolRepository.findByDomain(domain);
        if (existing.isPresent()) {
            return new CreateSchoolResult(SchoolDTO.from(existing.get()), true);
        }

        SchoolEntity school = new SchoolEntity();
        school.setName(name);
        school.setDomain(domain);
        school.setAdminApproved(true);

        return new CreateSchoolResult(SchoolDTO.from(schoolRepository.save(school)), false);
    }

    @Transactional
    public void deleteSchool(Long schoolId) {
        SchoolEntity school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"));

        List<UserEntity> users = userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(schoolId);
        for (UserEntity user : users) {
            userDeletionService.deleteUser(user);
        }

        schoolClassRepository.deleteAll(schoolClassRepository.findAllBySchool_IdOrderByNameAsc(schoolId));

        schoolIntegrationRepository.findBySchool_Id(schoolId).ifPresent(schoolIntegrationRepository::delete);

        schoolRepository.delete(school);
    }

    public SchoolDTO approveSchool(Long schoolId, ApproveSchoolRequest request) {
        SchoolEntity school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"));

        String name = request.name() != null ? request.name().trim() : "";
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schoolnaam is verplicht");

        school.setName(name);
        school.setAdminApproved(true);

        return SchoolDTO.from(schoolRepository.save(school));
    }
}
