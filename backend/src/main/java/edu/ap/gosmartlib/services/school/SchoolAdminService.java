package edu.ap.gosmartlib.services.school;

import edu.ap.gosmartlib.dto.school.ApproveSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolRequest;
import edu.ap.gosmartlib.dto.school.SchoolDTO;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolAdminService {

    private final SchoolRepository schoolRepository;

    public List<SchoolDTO> listAllSchools() {
        return schoolRepository.findAllByOrderByAdminApprovedAscNameAsc()
                .stream()
                .map(SchoolDTO::from)
                .toList();
    }

    public SchoolDTO createSchool(CreateSchoolRequest request) {
        String name = request.name() != null ? request.name().trim() : "";
        String domain = request.domain() != null ? request.domain().trim() : "";

        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schoolnaam is verplicht");
        if (domain.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schooldomein is verplicht");
        if (schoolRepository.existsByDomain(domain)) throw new ResponseStatusException(HttpStatus.CONFLICT, "School met dit domein bestaat al");

        SchoolEntity school = new SchoolEntity();
        school.setName(name);
        school.setDomain(domain);
        school.setAdminApproved(true);

        return SchoolDTO.from(schoolRepository.save(school));
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
