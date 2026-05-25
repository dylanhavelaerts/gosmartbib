package edu.ap.gosmartlib.services.schoolIntegration;

import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveClassesResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationLiveUsersResponse;
import edu.ap.gosmartlib.dto.schoolIntegration.SchoolIntegrationTestResponse;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolIntegrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolIntegrationAdminServiceTest {

    @Mock
    private SchoolIntegrationService schoolIntegrationService;

    @Mock
    private SmartschoolOneRosterAuthService authService;

    @Mock
    private SmartschoolOneRosterClient oneRosterClient;

    @Mock
    private SchoolIntegrationRepository schoolIntegrationRepository;

    @InjectMocks
    private SchoolIntegrationAdminService schoolIntegrationAdminService;

    @Test
    void givenEnabledIntegration_whenGetLiveUsers_thenReturnsFetchedUsers() {
        SchoolIntegrationEntity integration = integration(true);

        List<Map<String, Object>> users = List.of(
                Map.of("identifier", "sof2.first.last", "givenName", "First", "familyName", "Last"),
                Map.of("identifier", "sof01.leerkracht", "givenName", "Team01", "familyName", "ITSOF"));

        when(schoolIntegrationService.getIntegrationEntityForBibbeheerder("admin-uid", 1L)).thenReturn(integration);
        when(authService.getAccessToken(integration)).thenReturn("token-123");
        when(oneRosterClient.getUsers(integration, "token-123")).thenReturn(users);

        SchoolIntegrationLiveUsersResponse response = schoolIntegrationAdminService.getLiveUsers("admin-uid", 1L);

        assertTrue(response.success());
        assertEquals(2, response.userCount());
        assertEquals(users, response.users());
        assertEquals("Live OneRoster gebruikers opgehaald", response.message());
    }

    @Test
    void givenDisabledIntegration_whenGetLiveUsers_thenReturnsDisabledResponse() {
        SchoolIntegrationEntity integration = integration(false);

        when(schoolIntegrationService.getIntegrationEntityForBibbeheerder("admin-uid", 1L)).thenReturn(integration);

        SchoolIntegrationLiveUsersResponse response = schoolIntegrationAdminService.getLiveUsers("admin-uid", 1L);

        assertFalse(response.success());
        assertEquals(0, response.userCount());
        assertTrue(response.users().isEmpty());
        assertEquals("Integratie is niet ingeschakeld", response.message());

        verify(authService, never()).getAccessToken(integration);
        verify(oneRosterClient, never()).getUsers(integration, "token");
    }

    @Test
    void givenSuccessfulConnection_whenTestIntegration_thenEnablesIntegrationAndSaves() {
        SchoolIntegrationEntity integration = integration(false);

        List<Map<String, Object>> schools = List.of(
                Map.of("sourcedId", "5905", "name", "AP Hogeschool"));

        when(schoolIntegrationService.getIntegrationEntityForBibbeheerder("admin-uid", 1L)).thenReturn(integration);
        when(authService.getAccessToken(integration)).thenReturn("token-123");
        when(oneRosterClient.getSchools(integration, "token-123")).thenReturn(schools);

        SchoolIntegrationTestResponse response = schoolIntegrationAdminService.testIntegration("admin-uid", 1L);

        assertTrue(response.success());
        assertTrue(response.schoolsEndpointReachable());
        assertTrue(response.tokenReceived());
        assertEquals(1, response.schoolCount());
        assertEquals("OneRoster verbinding werkt", response.message());

        ArgumentCaptor<SchoolIntegrationEntity> captor = ArgumentCaptor.forClass(SchoolIntegrationEntity.class);
        verify(schoolIntegrationRepository, times(1)).save(captor.capture());

        SchoolIntegrationEntity saved = captor.getValue();
        assertTrue(saved.isOnerosterEnabled());
        assertEquals(null, saved.getLastError());
        assertNotNull(saved.getLastTestSuccessfulAt());
    }

    @Test
    void givenClassesCallFails_whenGetLiveClasses_thenReturnsFailureResponse() {
        SchoolIntegrationEntity integration = integration(true);

        when(schoolIntegrationService.getIntegrationEntityForBibbeheerder("admin-uid", 1L)).thenReturn(integration);
        when(authService.getAccessToken(integration)).thenReturn("token-123");
        when(oneRosterClient.getClasses(integration, "token-123"))
                .thenThrow(new RuntimeException("401 Unauthorized"));

        SchoolIntegrationLiveClassesResponse response = schoolIntegrationAdminService.getLiveClasses("admin-uid", 1L);

        assertFalse(response.success());
        assertEquals(0, response.classCount());
        assertTrue(response.classes().isEmpty());
        assertTrue(response.message().contains("401 Unauthorized"));
    }

    private SchoolIntegrationEntity integration(boolean enabled) {
        SchoolEntity school = new SchoolEntity();
        school.setId(1L);
        school.setName("AP Hogeschool");

        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();
        integration.setSchool(school);
        integration.setOnerosterEnabled(enabled);
        integration.setLastTestSuccessfulAt((LocalDateTime) null);
        return integration;
    }
}