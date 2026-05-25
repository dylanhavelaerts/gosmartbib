package edu.ap.gosmartlib.services.messages;

import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolSoapClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartschoolMessageServiceTest {

    @Mock
    private SmartschoolOneRosterAuthService authService;
    @Mock
    private SmartschoolOneRosterClient oneRosterClient;
    @Mock
    private SchoolIntegrationRepository schoolIntegrationRepository;
    @Mock
    private SmartschoolSoapClient soapClient;

    @InjectMocks
    private SmartschoolMessageService smartschoolMessageService;

    @Test
    void givenNullOnerosterSourcedId_whenSendMessage_thenSkipsOneRosterAndSoapCall() {
        UserEntity user = buildUser(null, buildSchool(1L));
        SchoolIntegrationEntity integration = buildIntegration();
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));

        assertDoesNotThrow(() -> smartschoolMessageService.sendMessage(user, "Titel", "Bericht"));

        verify(oneRosterClient, never()).getUserBySourcedId(any(), any(), any());
        verify(soapClient, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    void givenBlankOnerosterSourcedId_whenSendMessage_thenSkipsOneRosterAndSoapCall() {
        UserEntity user = buildUser("  ", buildSchool(1L));
        SchoolIntegrationEntity integration = buildIntegration();
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));

        assertDoesNotThrow(() -> smartschoolMessageService.sendMessage(user, "Titel", "Bericht"));

        verify(oneRosterClient, never()).getUserBySourcedId(any(), any(), any());
        verify(soapClient, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    void givenNoSchoolIntegration_whenSendMessage_thenThrowsIllegalStateException() {
        UserEntity user = buildUser("sourced-id-1", buildSchool(1L));
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> smartschoolMessageService.sendMessage(user, "Titel", "Bericht"));

        verifyNoInteractions(soapClient);
    }

    @Test
    void givenNoUsernameFromOneRoster_whenSendMessage_thenSkipsSoapCall() {
        UserEntity user = buildUser("sourced-id-1", buildSchool(1L));
        SchoolIntegrationEntity integration = buildIntegration();
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));
        when(authService.getAccessToken(integration)).thenReturn("access-token");

        assertDoesNotThrow(() -> smartschoolMessageService.sendMessage(user, "Titel", "Bericht"));

        verify(soapClient, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    void givenValidUser_whenSendMessage_thenDelegatesToSoapClient() {
        UserEntity user = buildUser("sourced-id-1", buildSchool(1L));
        SchoolIntegrationEntity integration = buildIntegration();

        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));
        when(authService.getAccessToken(integration)).thenReturn("access-token");
        when(oneRosterClient.getUsers(integration, "access-token"))
                .thenReturn(List.of(
                        Map.of(
                                "sourcedId", "sourced-id-1",
                                "username", "jan.janssen")));

        smartschoolMessageService.sendMessage(user, "Boek beschikbaar", "Inhoud");

        verify(soapClient, times(1))
                .sendMessage(integration, "jan.janssen", "Boek beschikbaar", "Inhoud");
    }

    // --- helpers ---

    private UserEntity buildUser(String onerosterSourcedId, SchoolEntity school) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setSmartschoolUid("uid-1");
        user.setOnerosterSourcedId(onerosterSourcedId);
        user.setSchool(school);
        return user;
    }

    private SchoolEntity buildSchool(Long id) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        return school;
    }

    private SchoolIntegrationEntity buildIntegration() {
        return new SchoolIntegrationEntity();
    }
}
