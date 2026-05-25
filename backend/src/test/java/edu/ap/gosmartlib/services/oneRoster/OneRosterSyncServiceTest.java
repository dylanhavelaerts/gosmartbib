package edu.ap.gosmartlib.services.oneRoster;

import edu.ap.gosmartlib.dto.sync.SyncResultDTO;
import edu.ap.gosmartlib.dto.sync.SyncSummaryDTO;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import edu.ap.gosmartlib.services.users.UserDeletionService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneRosterSyncServiceTest {

    @Mock private SmartschoolOneRosterAuthService authService;
    @Mock private SmartschoolOneRosterClient client;
    @Mock private UserRepository userRepository;
    @Mock private UserDeletionService userDeletionService;
    @Mock private SchoolIntegrationRepository schoolIntegrationRepository;
    @Mock private SchoolClassRepository schoolClassRepository;

    @InjectMocks
    private OneRosterSyncService syncService;

    // region syncAll

    @Test
    void givenNoEnabledIntegrations_whenSyncAll_thenReturnsEmptySummary() {
        when(schoolIntegrationRepository.findAllByOnerosterEnabledTrue()).thenReturn(List.of());

        SyncSummaryDTO result = syncService.syncAll();

        assertEquals(0, result.totalAdded());
        assertEquals(0, result.totalRemoved());
        assertTrue(result.schools().isEmpty());
        verifyNoInteractions(authService, client, userRepository, userDeletionService);
    }

    @Test
    void givenTwoIntegrations_whenSyncAll_thenAggregatesResults() {
        SchoolEntity school1 = school(1L, "school1.be");
        SchoolEntity school2 = school(2L, "school2.be");

        SchoolIntegrationEntity int1 = integration(school1);
        SchoolIntegrationEntity int2 = integration(school2);

        when(schoolIntegrationRepository.findAllByOnerosterEnabledTrue()).thenReturn(List.of(int1, int2));
        when(authService.getAccessToken(any())).thenReturn("token");
        when(client.getUsers(any(), any())).thenReturn(List.of());
        when(client.getClasses(any(), any())).thenReturn(List.of());
        when(client.getEnrollments(any(), any())).thenReturn(List.of());
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(any())).thenReturn(List.of());

        SyncSummaryDTO result = syncService.syncAll();

        assertEquals(2, result.schools().size());
        verify(schoolIntegrationRepository, times(2)).save(any());
    }

    // endregion

    // region syncSchool — new users

    @Test
    void givenNewUserInOneRoster_whenSyncSchool_thenCreatesUser() {
        SchoolEntity school = school(1L, "school.be");
        SchoolIntegrationEntity integration = integration(school);

        Map<String, Object> onerosterUser = onerosterUser("uid-new", "src-1", "student");

        when(authService.getAccessToken(integration)).thenReturn("token");
        when(client.getUsers(integration, "token")).thenReturn(List.of(onerosterUser));
        when(client.getClasses(any(), any())).thenReturn(List.of());
        when(client.getEnrollments(any(), any())).thenReturn(List.of());
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(1L)).thenReturn(List.of());

        SyncResultDTO result = syncService.syncSchool(integration);

        assertEquals(1, result.added());
        assertEquals(0, result.removed());
        assertTrue(result.errors().isEmpty());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity created = captor.getValue();
        assertEquals("uid-new", created.getSmartschoolUid());
        assertEquals("src-1", created.getOnerosterSourcedId());
        assertEquals(UserRoles.STUDENT, created.getRole());
        assertSame(school, created.getSchool());
    }

    @Test
    void givenExistingUserAlsoInOneRoster_whenSyncSchool_thenNeitherCreatesNorDeletes() {
        SchoolEntity school = school(1L, "school.be");
        SchoolIntegrationEntity integration = integration(school);

        UserEntity existing = user("uid-existing", school);
        Map<String, Object> onerosterUser = onerosterUser("uid-existing", "src-1", "student");

        when(authService.getAccessToken(integration)).thenReturn("token");
        when(client.getUsers(integration, "token")).thenReturn(List.of(onerosterUser));
        when(client.getClasses(any(), any())).thenReturn(List.of());
        when(client.getEnrollments(any(), any())).thenReturn(List.of());
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(1L)).thenReturn(List.of(existing));

        SyncResultDTO result = syncService.syncSchool(integration);

        assertEquals(0, result.added());
        assertEquals(0, result.removed());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userDeletionService);
    }

    // endregion

    // region syncSchool — removed users

    @Test
    void givenUserInDbNotInOneRoster_whenSyncSchool_thenDeletesUser() {
        SchoolEntity school = school(1L, "school.be");
        SchoolIntegrationEntity integration = integration(school);

        UserEntity stale = user("uid-stale", school);

        when(authService.getAccessToken(integration)).thenReturn("token");
        when(client.getUsers(integration, "token")).thenReturn(List.of());
        when(client.getClasses(any(), any())).thenReturn(List.of());
        when(client.getEnrollments(any(), any())).thenReturn(List.of());
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(1L)).thenReturn(List.of(stale));

        SyncResultDTO result = syncService.syncSchool(integration);

        assertEquals(0, result.added());
        assertEquals(1, result.removed());
        verify(userDeletionService).deleteUser(stale);
    }

    // endregion

    // region syncSchool — error handling

    @Test
    void givenOneRosterThrows_whenSyncSchool_thenRecordsErrorAndReturnsZeroCounts() {
        SchoolEntity school = school(1L, "school.be");
        SchoolIntegrationEntity integration = integration(school);

        when(authService.getAccessToken(integration)).thenThrow(new RuntimeException("token error"));

        SyncResultDTO result = syncService.syncSchool(integration);

        assertEquals(0, result.added());
        assertEquals(0, result.removed());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("token error"));
        verify(schoolIntegrationRepository).save(integration);
    }

    @Test
    void givenOneRosterUserWithoutLegacyIdentifier_whenSyncSchool_thenSkipsUser() {
        SchoolEntity school = school(1L, "school.be");
        SchoolIntegrationEntity integration = integration(school);

        Map<String, Object> noUidUser = Map.of("identifier", "no-legacy", "role", "student");

        when(authService.getAccessToken(integration)).thenReturn("token");
        when(client.getUsers(integration, "token")).thenReturn(List.of(noUidUser));
        when(client.getClasses(any(), any())).thenReturn(List.of());
        when(client.getEnrollments(any(), any())).thenReturn(List.of());
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(1L)).thenReturn(List.of());

        SyncResultDTO result = syncService.syncSchool(integration);

        assertEquals(0, result.added());
        verify(userRepository, never()).save(any());
    }

    @Test
    void givenSyncSucceeds_whenSyncSchool_thenUpdatesLastSyncAt() {
        SchoolEntity school = school(1L, "school.be");
        SchoolIntegrationEntity integration = integration(school);

        when(authService.getAccessToken(integration)).thenReturn("token");
        when(client.getUsers(integration, "token")).thenReturn(List.of());
        when(client.getClasses(any(), any())).thenReturn(List.of());
        when(client.getEnrollments(any(), any())).thenReturn(List.of());
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(1L)).thenReturn(List.of());

        syncService.syncSchool(integration);

        assertNotNull(integration.getLastSyncAt());
        assertNull(integration.getLastError());
        verify(schoolIntegrationRepository).save(integration);
    }

    // endregion

    // region helpers

    private SchoolEntity school(Long id, String domain) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        school.setName("School " + id);
        school.setDomain(domain);
        return school;
    }

    private SchoolIntegrationEntity integration(SchoolEntity school) {
        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();
        integration.setSchool(school);
        integration.setOnerosterEnabled(true);
        return integration;
    }

    private UserEntity user(String uid, SchoolEntity school) {
        UserEntity user = new UserEntity();
        user.setSmartschoolUid(uid);
        user.setSchool(school);
        user.setRole(UserRoles.STUDENT);
        return user;
    }

    private Map<String, Object> onerosterUser(String legacyUid, String sourcedId, String role) {
        return Map.of(
                "sourcedId", sourcedId,
                "role", role,
                "metadata", Map.of("smsc.legacyIdentifier", legacyUid));
    }

    // endregion
}
