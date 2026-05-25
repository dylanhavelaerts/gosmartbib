package edu.ap.gosmartlib.services.messages;

import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolSoapClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@Slf4j
@RequiredArgsConstructor
public class SmartschoolMessageService implements MessageSender {

    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient oneRosterClient;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final SmartschoolSoapClient soapClient;

    public void sendMessage(UserEntity user, String title, String body) {
        if (user.getSchool() == null) {
            log.warn("Gebruiker {} heeft geen school gekoppeld, bericht niet verstuurd", user.getId());
            return;
        }
        SchoolIntegrationEntity integration = schoolIntegrationRepository
                .findBySchool_Id(user.getSchool().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "School heeft geen integratie ingesteld voor school met id " + user.getSchool().getId()));

        if (user.getOnerosterSourcedId() == null || user.getOnerosterSourcedId().isBlank()) {
            log.warn("Gebruiker {} heeft geen onerosterSourcedId, bericht niet verstuurd", user.getId());
            return;
        }

        String accessToken = authService.getAccessToken(integration);

        String username = oneRosterClient.getUsers(integration, accessToken).stream()
                .filter(u -> user.getOnerosterSourcedId().equals(u.get("sourcedId")))
                .map(u -> (String) u.get("username"))
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);

        if (username == null || username.isBlank()) {
            log.warn("Geen username gevonden voor gebruiker {}", user.getId());
            return;
        }

        soapClient.sendMessage(integration, username, title, body);
    }




    }



