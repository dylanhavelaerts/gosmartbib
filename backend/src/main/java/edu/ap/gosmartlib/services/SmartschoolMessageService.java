package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolSoapClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmartschoolMessageService {

    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient oneRosterClient;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final SmartschoolSoapClient soapClient;

    public void sendMessage(UserEntity user, String title, String body){
        SchoolIntegrationEntity integration = schoolIntegrationRepository
                .findBySchool_Id(user.getSchool().getId())
                .orElseThrow(() -> new IllegalStateException("School heeft geen integratie ingesteld voor school met id" + user.getSchool().getId()));
        String accessToken = authService.getAccessToken(integration);
        if (user.getOnerosterSourcedId() == null || user.getOnerosterSourcedId().isBlank()) {
            log.warn("Gebruiker {} heeft geen onerosterSourcedId, bericht niet verstuurd", user.getId());
            return;
        }
        Map<String, Object> userDetails = oneRosterClient
                .getUserBySourcedId(integration, accessToken, user.getOnerosterSourcedId());
        String username = (String) userDetails.get("username");
        if (username == null||username.isBlank()){
            log.warn("Geen username gevonden voor gebruiker {}", user.getId());
            return;
        }
        soapClient.sendMessage(integration, username, title, body);

    }



    }



