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

/**
 * Service voor het testen en previewen van schoolintegraties.
 *
 * <p>
 * Deze service haalt live OneRoster-data op uit Smartschool zonder die
 * rechtstreeks in de databank op te slaan. De effectieve synchronisatie gebeurt
 * via {@link OneRosterSyncService}.
 * </p>
 */
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

    /**
     * Haalt live klassen op uit OneRoster voor een bestaande integratie.
     *
     * <p>
     * Deze methode wordt gebruikt als preview/testfunctie en slaat de opgehaalde
     * klassen niet op in de lokale databank.
     * </p>
     *
     * @param integration de schoolintegratie waarvoor klassen opgehaald worden
     * @return response met live klassen of een foutmelding
     */
    private SchoolIntegrationLiveClassesResponse performGetLiveClasses(SchoolIntegrationEntity integration) {
        if (!integration.isOnerosterEnabled())
            return new SchoolIntegrationLiveClassesResponse(false, 0, List.of(), "Integratie is niet ingeschakeld");
        try {
            String token = authService.getAccessToken(integration);
            List<Map<String, Object>> classes = oneRosterClient.getClasses(integration, token);
            return new SchoolIntegrationLiveClassesResponse(true, classes.size(), classes,
                    "Live OneRoster klassen opgehaald");
        } catch (Exception ex) {
            return new SchoolIntegrationLiveClassesResponse(false, 0, List.of(),
                    "Ophalen van klassen mislukt: " + ex.getMessage());
        }
    }

    /**
     * Test de OneRoster-verbinding voor een schoolintegratie.
     *
     * <p>
     * De methode vraagt eerst een access token aan en controleert daarna of
     * het schools-endpoint bereikbaar is. Bij succes wordt de teststatus opgeslagen
     * en wordt de OneRoster-synchronisatie gestart wanneer de integratie actief is.
     * </p>
     *
     * @param integration de integratie die getest wordt
     * @return resultaat van de verbindingstest
     */
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

    /**
     * Haalt live gebruikers op uit OneRoster voor een bestaande integratie.
     *
     * <p>
     * Deze methode wordt gebruikt als preview/testfunctie en slaat de opgehaalde
     * gebruikers niet op in de lokale databank.
     * </p>
     *
     * @param integration de schoolintegratie waarvoor gebruikers opgehaald worden
     * @return response met live gebruikers of een foutmelding
     */
    private SchoolIntegrationLiveUsersResponse performGetLiveUsers(SchoolIntegrationEntity integration) {
        if (!integration.isOnerosterEnabled())
            return new SchoolIntegrationLiveUsersResponse(false, 0, List.of(), "Integratie is niet ingeschakeld");
        try {
            String token = authService.getAccessToken(integration);
            List<Map<String, Object>> users = oneRosterClient.getUsers(integration, token);
            return new SchoolIntegrationLiveUsersResponse(true, users.size(), users,
                    "Live OneRoster gebruikers opgehaald");
        } catch (Exception ex) {
            return new SchoolIntegrationLiveUsersResponse(false, 0, List.of(),
                    "Ophalen van gebruikers mislukt: " + ex.getMessage());
        }
    }

    /**
     * Haalt live scholen of organisaties op uit OneRoster.
     *
     * <p>
     * Deze methode wordt gebruikt om te controleren of de OneRoster-configuratie
     * naar de juiste Smartschoolomgeving verwijst.
     * </p>
     *
     * @param integration de schoolintegratie waarvoor scholen opgehaald worden
     * @return response met live scholen/organisaties
     */
    private SchoolIntegrationLiveSchoolsResponse performGetLiveSchools(SchoolIntegrationEntity integration) {
        if (!integration.isOnerosterEnabled())
            return new SchoolIntegrationLiveSchoolsResponse(false, 0, List.of(), "Integratie is niet ingeschakeld");
        String token = authService.getAccessToken(integration);
        List<Map<String, Object>> schools = oneRosterClient.getSchools(integration, token);
        return new SchoolIntegrationLiveSchoolsResponse(true, schools.size(), schools,
                "Live OneRoster scholen opgehaald");
    }
}
