package edu.ap.gosmartlib.services.schoolIntegration;

import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartschoolOneRosterClient {

    private final RestClient restClient = RestClient.create();

    public List<Map<String, Object>> getSchools(SchoolIntegrationEntity integration, String accessToken) {
        return getCollection(integration, accessToken, "/ims/oneroster/v1p1/schools", "orgs", "schools");
    }

    public List<Map<String, Object>> getUsers(SchoolIntegrationEntity integration, String accessToken) {
        return getCollection(integration, accessToken, "/ims/oneroster/v1p1/users", "users");
    }

    public List<Map<String, Object>> getClasses(SchoolIntegrationEntity integration, String accessToken) {
        return getCollection(integration, accessToken, "/ims/oneroster/v1p1/classes", "classes");
    }

    public Map<String, Object> getUserBySourcedId(SchoolIntegrationEntity integration, String accessToken, String sourcedId) {
        return getSingle(integration, accessToken, "/ims/oneroster/v1p1/users/" + sourcedId, "user");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCollection(
            SchoolIntegrationEntity integration,
            String accessToken,
            String path,
            String... responseKeys) {

        ResponseEntity<Map> response = restClient.get()
                .uri(integration.getOnerosterBaseUrl() + path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toEntity(Map.class);

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
    @SuppressWarnings("unchecked")
    private Map<String, Object> getSingle(
            SchoolIntegrationEntity integration,
            String accessToken,
            String path,
            String responseKey) {

        try {
            ResponseEntity<Map> response = restClient.get()
                    .uri(integration.getOnerosterBaseUrl() + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toEntity(Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) return Map.of();

            Object value = body.get(responseKey);
            if (value instanceof Map<?, ?>) {
                return (Map<String, Object>) value;
            }
            return Map.of();
        } catch (Exception e) {
            log.error("OneRoster getSingle mislukt voor {}: {}", path, e.getMessage());
            return Map.of();
        }
    }

}