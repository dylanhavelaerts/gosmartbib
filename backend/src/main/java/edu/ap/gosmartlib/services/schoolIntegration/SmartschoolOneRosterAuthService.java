package edu.ap.gosmartlib.services.schoolIntegration;

import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartschoolOneRosterAuthService {

    private final RestClient restClient = RestClient.create();

    public String getAccessToken(SchoolIntegrationEntity integration) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", integration.getOnerosterClientId());
        form.add("client_secret", integration.getOnerosterClientSecret());
        form.add("scope", "https://purl.imsglobal.org/spec/or/v1p1/scope/roster-core.readonly");

        Map<?, ?> response = restClient.post()
                .uri(integration.getOnerosterBaseUrl() + "/ims/oneroster/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Geen access token ontvangen van Smartschool");
        }

        return response.get("access_token").toString();
    }
}