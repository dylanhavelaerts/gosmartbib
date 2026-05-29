package edu.ap.gosmartlib.services.schoolintegration;

import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveClassesResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveSchoolsResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveUsersResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationTestResponse;
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import edu.ap.gosmartlib.repositories.school.SchoolIntegrationRepository;
import edu.ap.gosmartlib.services.oneroster.OneRosterSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchoolIntegrationAdminService {

    private final SchoolIntegrationService schoolIntegrationService;
    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient oneRosterClient;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final OneRosterSyncService oneRosterSyncService;

    @Transactional
    public SchoolIntegrationTestResponse testIntegration(String actorUid, Long schoolId) {
        return performTest(schoolIntegrationService.getIntegrationEntityForLibrarian(actorUid, schoolId));
    }

    @Transactional
    public SchoolIntegrationTestResponse testIntegrationForPlatformAdmin(Long schoolId) {
        return performTest(schoolIntegrationService.getIntegrationEntityForPlatformAdmin(schoolId));
    }



    @Transactional(readOnly = true)
    public SchoolIntegrationLiveSchoolsResponse getLiveSchools(String actorUid, Long schoolId) {
        return performGetLiveSchools(schoolIntegrationService.getIntegrationEntityForLibrarian(actorUid, schoolId));
    }

    public SchoolIntegrationLiveSchoolsResponse getLiveSchoolsForPlatformAdmin(Long schoolId) {
        return performGetLiveSchools(schoolIntegrationService.getIntegrationEntityForPlatformAdmin(schoolId));
    }



    public SchoolIntegrationLiveUsersResponse getLiveUsers(String actorUid, Long schoolId) {
        return performGetLiveUsers(schoolIntegrationService.getIntegrationEntityForLibrarian(actorUid, schoolId));
    }

    public SchoolIntegrationLiveUsersResponse getLiveUsersForPlatformAdmin(Long schoolId) {
        return performGetLiveUsers(schoolIntegrationService.getIntegrationEntityForPlatformAdmin(schoolId));
    }



    public SchoolIntegrationLiveClassesResponse getLiveClasses(String actorUid, Long schoolId) {
        return performGetLiveClasses(schoolIntegrationService.getIntegrationEntityForLibrarian(actorUid, schoolId));
    }

    public SchoolIntegrationLiveClassesResponse getLiveClassesForPlatformAdmin(Long schoolId) {
        return performGetLiveClasses(schoolIntegrationService.getIntegrationEntityForPlatformAdmin(schoolId));
    }

    private SchoolIntegrationLiveClassesResponse performGetLiveClasses(SchoolIntegrationEntity integration) {
        if (!integration.isOnerosterEnabled())
            return new SchoolIntegrationLiveClassesResponse(false, 0, List.of(), "Integratie is niet ingeschakeld");
        try {
            String token = authService.getAccessToken(integration);
            List<Map<String, Object>> classes = oneRosterClient.getClasses(integration, token);
            return new SchoolIntegrationLiveClassesResponse(true, classes.size(), classes, "Live OneRoster klassen opgehaald");
        } catch (Exception ex) {
            return new SchoolIntegrationLiveClassesResponse(false, 0, List.of(), "Ophalen van klassen mislukt: " + ex.getMessage());
        }
    }
    private SchoolIntegrationTestResponse performTest(SchoolIntegrationEntity integration) {
        SchoolIntegrationTestResponse response;
        try {
            String token = authService.getAccessToken(integration);
            List<Map<String, Object>> schools = oneRosterClient.getSchools(integration, token);
            integration.setLastTestSuccessfulAt(LocalDateTime.now());
            integration.setLastError(null);
            integration.setOnerosterEnabled(true);
            schoolIntegrationRepository.save(integration);
            response = new SchoolIntegrationTestResponse(
                    true,
                    true,
                    true,
                    schools.size(),
                    "OneRoster verbinding werkt");
        } catch (Exception ex) {
            integration.setLastError(ex.getMessage());
            schoolIntegrationRepository.save(integration);

            response = new SchoolIntegrationTestResponse(
                    false,
                    false,
                    false,
                    0,
                    "Test mislukt: " + ex.getMessage());
        }

        if (integration.isOnerosterEnabled()) {
            oneRosterSyncService.syncSchool(integration);
        }

        return response;
    }
    private SchoolIntegrationLiveUsersResponse performGetLiveUsers(SchoolIntegrationEntity integration) {
        if (!integration.isOnerosterEnabled())
            return new SchoolIntegrationLiveUsersResponse(false, 0, List.of(), "Integratie is niet ingeschakeld");
        try {
            String token = authService.getAccessToken(integration);
            List<Map<String, Object>> users = oneRosterClient.getUsers(integration, token);
            return new SchoolIntegrationLiveUsersResponse(true, users.size(), users, "Live OneRoster gebruikers opgehaald");
        } catch (Exception ex) {
            return new SchoolIntegrationLiveUsersResponse(false, 0, List.of(), "Ophalen van gebruikers mislukt: " + ex.getMessage());
        }
    }
    private SchoolIntegrationLiveSchoolsResponse performGetLiveSchools(SchoolIntegrationEntity integration) {
        if (!integration.isOnerosterEnabled())
            return new SchoolIntegrationLiveSchoolsResponse(false, 0, List.of(), "Integratie is niet ingeschakeld");
        String token = authService.getAccessToken(integration);
        List<Map<String, Object>> schools = oneRosterClient.getSchools(integration, token);
        return new SchoolIntegrationLiveSchoolsResponse(true, schools.size(), schools, "Live OneRoster scholen opgehaald");
    }
}
