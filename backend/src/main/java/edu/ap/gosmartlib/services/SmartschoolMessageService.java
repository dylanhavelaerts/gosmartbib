package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmartschoolMessageService {

    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient oneRosterClient;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final RestClient restClient = RestClient.create();

    @Async
    public void sendMessage(UserEntity user, String title, String body){
        SchoolIntegrationEntity integration = schoolIntegrationRepository
                .findBySchool_Id(user.getSchool().getId())
                .orElseThrow(() -> new IllegalStateException("School heeft geen integratie ingesteld voor school met id" + user.getSchool().getId()));
        String accessToken = authService.getAccessToken(integration);

        Map<String, Object> userDetails = oneRosterClient
                .getUserBySourcedId(integration, accessToken, user.getOnerosterSourcedId());
        String username = (String) userDetails.get("username");
        if (username == null||username.isBlank()){
            log.warn("Geen username gevonden voor gebruiker {}", user.getId());
            return;
        }
        sendSoapMessage(integration, username, title, body);
    }

    //Hardcode om te testen -> volledige methode gaat weg
    @Async
    public void sendTestMessage(String username) {
        SchoolIntegrationEntity integration = schoolIntegrationRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Geen integratie gevonden"));

        sendSoapMessage(integration, username, "Test bericht", "Dit is een testbericht vanuit GoSmartLib.");
    }
    private void sendSoapMessage(SchoolIntegrationEntity integration, String username, String title, String body){

        String accesscode = integration.getSmartschoolAccesscode();
        String endpoint = integration.getOnerosterBaseUrl() + "/Webservices/V3";

        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:ss="http://www.smartschool.be/Webservices">
                  <soapenv:Body>
                    <ss:sendMsg>
                      <accesscode>%s</accesscode>
                      <userIdentifier>%s</userIdentifier>
                      <title>%s</title>
                      <body>%s</body>
                      <senderIdentifier>sof2.benjamin.deloore</senderIdentifier>
                    </ss:sendMsg>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(accesscode, username, title, body);

        String response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.TEXT_XML)
                .body(soapEnvelope)
                .retrieve()
                .body(String.class);

        log.info("Smartschool SOAP response voor {}: {}", username, response);
    }
}
