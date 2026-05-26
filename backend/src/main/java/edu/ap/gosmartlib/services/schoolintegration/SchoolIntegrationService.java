package edu.ap.gosmartlib.services.schoolintegration;

import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationDTO;
import edu.ap.gosmartlib.dto.schoolintegration.UpsertSchoolIntegrationRequest;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.school.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.oneroster.OneRosterSyncService;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SchoolIntegrationService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final OneRosterSyncService oneRosterSyncService;

    @Transactional(readOnly = true)
    public SchoolIntegrationDTO getIntegration(String actorUid, Long schoolId) {
        getCurrentBibbeheerder(actorUid);

        SchoolIntegrationEntity integration = schoolIntegrationRepository.findBySchool_Id(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integratie niet gevonden"));

        return SchoolIntegrationDTO.from(integration);
    }

    @Transactional
    public SchoolIntegrationDTO upsertIntegration(String actorUid, Long schoolId,
            UpsertSchoolIntegrationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body ontbreekt");
        }

        getCurrentBibbeheerder(actorUid);

        validateRequest(request);

        SchoolEntity school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"));

        SchoolIntegrationEntity integration = schoolIntegrationRepository.findBySchool_Id(schoolId)
                .orElseGet(SchoolIntegrationEntity::new);

        integration.setSchool(school);
        integration.setSchoolBaseUrl(normalizeBaseUrl(request.schoolBaseUrl()));
        integration.setOnerosterClientId(request.onerosterClientId().trim());

        if (request.onerosterClientSecret() != null && !request.onerosterClientSecret().isBlank()) {
            integration.setOnerosterClientSecret(request.onerosterClientSecret().trim());
        } else if (integration.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client secret ontbreekt");
        }

        if (request.onerosterEnabled() != null) {
            integration.setOnerosterEnabled(request.onerosterEnabled());
        }
        if (request.smartschoolAccesscode() != null && !request.smartschoolAccesscode().isBlank()) {
            integration.setSmartschoolAccesscode(request.smartschoolAccesscode().trim());
        }


        integration = schoolIntegrationRepository.save(integration);

        if (integration.isOnerosterEnabled()) {
            oneRosterSyncService.syncSchool(integration);
        }

        return SchoolIntegrationDTO.from(integration);
    }

    protected UserEntity getCurrentBibbeheerder(String actorUid) {
        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingelogde gebruiker niet gevonden"));
        if (actor.getRole() != UserRoles.BIBLIOTHEEKBEHEERDER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang");
        return actor;
    }

    @Transactional(readOnly = true)
    public SchoolIntegrationEntity getIntegrationEntityForBibbeheerder(String actorUid, Long schoolId) {
        getCurrentBibbeheerder(actorUid);

        return schoolIntegrationRepository.findBySchool_Id(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integratie niet gevonden"));
    }
    @Transactional(readOnly = true)
    public SchoolIntegrationDTO getIntegrationForPlatformAdmin(Long schoolId) {
        return SchoolIntegrationDTO.from(schoolIntegrationRepository.findBySchool_Id(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integratie niet gevonden")));
    }

    @Transactional
    public SchoolIntegrationDTO upsertIntegrationForPlatformAdmin(Long schoolId, UpsertSchoolIntegrationRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body ontbreekt");
        validateRequest(request);

        SchoolEntity school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"));

        SchoolIntegrationEntity integration = schoolIntegrationRepository.findBySchool_Id(schoolId)
                .orElseGet(SchoolIntegrationEntity::new);

        integration.setSchool(school);
        integration.setSchoolBaseUrl(normalizeBaseUrl(request.schoolBaseUrl()));
        integration.setOnerosterClientId(request.onerosterClientId().trim());

        if (request.onerosterClientSecret() != null && !request.onerosterClientSecret().isBlank())
            integration.setOnerosterClientSecret(request.onerosterClientSecret().trim());
        else if (integration.getId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client secret ontbreekt");

        if (request.onerosterEnabled() != null) integration.setOnerosterEnabled(request.onerosterEnabled());
        if (request.smartschoolAccesscode() != null && !request.smartschoolAccesscode().isBlank())
            integration.setSmartschoolAccesscode(request.smartschoolAccesscode().trim());

        integration = schoolIntegrationRepository.save(integration);

        if (integration.isOnerosterEnabled()) {
            oneRosterSyncService.syncSchool(integration);
        }

        return SchoolIntegrationDTO.from(integration);
    }

    @Transactional(readOnly = true)
    public SchoolIntegrationEntity getIntegrationEntityForPlatformAdmin(Long schoolId) {
        return schoolIntegrationRepository.findBySchool_Id(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integratie niet gevonden"));
    }


    private void validateRequest(UpsertSchoolIntegrationRequest request) {
        if (request.schoolBaseUrl() == null || request.schoolBaseUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OneRoster base URL ontbreekt");
        }
        if (request.onerosterClientId() == null || request.onerosterClientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OneRoster client ID ontbreekt");
        }
    }

    private String normalizeBaseUrl(String value) {
        String url = value.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}