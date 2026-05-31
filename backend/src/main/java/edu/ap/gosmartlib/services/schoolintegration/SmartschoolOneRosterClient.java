package edu.ap.gosmartlib.services.schoolintegration;

import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client voor het uitlezen van Smartschool OneRoster endpoints.
 *
 * <p>
 * Deze client voert GET-requests uit naar de OneRoster v1.1 API en geeft
 * de ontvangen payloads terug als maps, omdat de response per endpoint een
 * andere structuur kan hebben.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartschoolOneRosterClient {

    private final RestClient restClient = RestClient.create();

    /**
     * Haalt scholen/organisaties op uit OneRoster.
     *
     * <p>
     * Deze call wordt gebruikt om te controleren of de Smartschool OneRoster-
     * verbinding correct werkt.
     * </p>
     *
     * @param integration de schoolintegratie met de Smartschool base URL
     * @param accessToken bearer token voor de OneRoster API
     * @return lijst met OneRoster-scholen of organisaties
     */
    public List<Map<String, Object>> getSchools(SchoolIntegrationEntity integration, String accessToken) {
        return getCollection(integration, accessToken, "/ims/oneroster/v1p1/schools", "orgs", "schools");
    }

    /**
     * Haalt alle gebruikers op uit OneRoster.
     *
     * @param integration de schoolintegratie met de Smartschool base URL
     * @param accessToken bearer token voor de OneRoster API
     * @return lijst met OneRoster-gebruikers
     */
    public List<Map<String, Object>> getUsers(SchoolIntegrationEntity integration, String accessToken) {
        return getCollection(integration, accessToken, "/ims/oneroster/v1p1/users", "users");
    }

    /**
     * Haalt alle klassen op uit OneRoster.
     *
     * @param integration de schoolintegratie met de Smartschool base URL
     * @param accessToken bearer token voor de OneRoster API
     * @return lijst met OneRoster-klassen
     */
    public List<Map<String, Object>> getClasses(SchoolIntegrationEntity integration, String accessToken) {
        return getCollection(integration, accessToken, "/ims/oneroster/v1p1/classes", "classes");
    }

    /**
     * Haalt alle inschrijvingen op uit OneRoster.
     *
     * <p>
     * Inschrijvingen bepalen welke gebruikers aan welke klassen gekoppeld zijn.
     * </p>
     *
     * @param integration de schoolintegratie met de Smartschool base URL
     * @param accessToken bearer token voor de OneRoster API
     * @return lijst met OneRoster-inschrijvingen
     */
    public List<Map<String, Object>> getEnrollments(SchoolIntegrationEntity integration, String accessToken) {
        return getCollection(integration, accessToken, "/ims/oneroster/v1p1/enrollments", "enrollments");
    }

    /**
     * Voert een GET-request uit naar een OneRoster collection endpoint.
     *
     * <p>
     * Smartschool kan collections onder verschillende response keys teruggeven,
     * bijvoorbeeld {@code orgs} of {@code schools}. Daarom krijgt deze methode
     * meerdere mogelijke keys mee en gebruikt ze de eerste key die een lijst bevat.
     * </p>
     *
     * @param integration  de schoolintegratie met de Smartschool base URL
     * @param accessToken  bearer token voor de OneRoster API
     * @param path         het OneRoster endpoint-pad
     * @param responseKeys mogelijke response keys waarin de collectie kan zitten
     * @return de gevonden collectie, of een lege lijst wanneer er niets gevonden
     *         wordt
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCollection(
            SchoolIntegrationEntity integration,
            String accessToken,
            String path,
            String... responseKeys) {

        ResponseEntity<Map<String, Object>> response = restClient.get()
                .uri(integration.getSchoolBaseUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Map<String, Object> body = response.getBody();
        if (body == null) {
            return List.of();
        }

        for (String key : responseKeys) {
            Object value = body.get(key);
            if (value instanceof List<?>) {
                return (List<Map<String, Object>>) value;
            }
        }

        return List.of();
    }
}