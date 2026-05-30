package edu.ap.gosmartlib.services.messages;

import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.HomepageSettingsRepository;
import edu.ap.gosmartlib.repositories.school.SchoolIntegrationRepository;
import edu.ap.gosmartlib.services.schoolintegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolintegration.SmartschoolOneRosterClient;
import edu.ap.gosmartlib.services.schoolintegration.SmartschoolSoapClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Verstuurt berichten via de Smartschool SOAP-API.
 * Resolvet de Smartschool-gebruikersnaam door alle OneRoster-gebruikers op te halen
 * en in Java te filteren op onerosterSourcedId.
 * getUserBySourcedId wordt niet gebruikt omdat Smartschool die te laat beschikbaar stelt
 * waardoor de gebruiker niet gevonden wordt.
 * Als de gebruiker geen school, geen onerosterSourcedId of geen overeenkomende gebruikersnaam heeft, wordt het bericht stilletjes overgeslagen.
 */
@Primary
@Service
@Slf4j
@RequiredArgsConstructor
public class SmartschoolMessageService implements MessageSender {

    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient oneRosterClient;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final SmartschoolSoapClient soapClient;
    private final HomepageSettingsRepository homepageSettingsRepository;


    /**
     * Haalt de Smartschool-gebruikersnaam op via een volledige OneRoster-gebruikerslijst en filtert op onerosterSourcedId.
     * Slaat het bericht over als de gebruiker geen school, geen onerosterSourcedId heeft of als er geen overeenkomende gebruikersnaam gevonden wordt.
     */
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
        String senderIdentifier = homepageSettingsRepository.findBySchool_Id(user.getSchool().getId())
                .map(s -> s.getSmartschoolSenderIdentifier())
                .orElse("");

        soapClient.sendMessage(integration, senderIdentifier, username, title, body);

    }




    }