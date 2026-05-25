package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesResponse;
<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryResolveDisplayNamesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolIntegrationRepository schoolIntegrationRepository;

    @Mock
    private SmartschoolOneRosterAuthService authService;

    @Mock
    private SmartschoolOneRosterClient oneRosterClient;

    @InjectMocks
    private UserDirectoryService userDirectoryService;

    @Test
    void givenLiveUserWithLegacyIdentifier_whenResolveDisplayNames_thenReturnsResolvedName() {
        SchoolEntity school = school(1L, "AP Hogeschool");

        UserEntity actor = user(1L, "admin-uid", UserRoles.ADMIN, school);
        UserEntity target = user(2L, "ouyzgpfoiahoih==", UserRoles.STUDENT, school);

        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();
        integration.setSchool(school);
        integration.setOnerosterEnabled(true);

        Map<String, Object> liveUser = Map.of(
                "identifier", "sof2.first.last",
                "username", "sof2.first.last",
                "givenName", "First",
                "familyName", "Last",
                "metadata", Map.of("smsc.legacyIdentifier", "ouyzgpfoiahoih=="));

        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findAllBySchool_IdAndSmartschoolUidIn(
                1L,
                List.of("ouyzgpfoiahoih=="))).thenReturn(List.of(target));
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));
        when(authService.getAccessToken(integration)).thenReturn("token-123");
        when(oneRosterClient.getUsers(integration, "token-123")).thenReturn(List.of(liveUser));

        ResolveDisplayNamesResponse response = userDirectoryService.resolveDisplayNames(
                "admin-uid",
                new ResolveDisplayNamesRequest(List.of("ouyzgpfoiahoih=="), 1L));

        assertTrue(response.success());
        assertEquals(1, response.requestedCount());
        assertEquals(1, response.resolvedCount());
        assertEquals("First Last", response.displayNames().get("ouyzgpfoiahoih=="));
        assertTrue(response.unresolvedUids().isEmpty());
    }

    @Test
    void givenDisabledIntegration_whenResolveDisplayNames_thenReturnsFailureWithoutLiveNames() {
        SchoolEntity school = school(1L, "AP Hogeschool");

        UserEntity actor = user(1L, "admin-uid", UserRoles.ADMIN, school);
        UserEntity target = user(2L, "ouyzgpfoiahoih==", UserRoles.STUDENT, school);

        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();
        integration.setSchool(school);
        integration.setOnerosterEnabled(false);

        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findAllBySchool_IdAndSmartschoolUidIn(
                1L,
                List.of("ouyzgpfoiahoih=="))).thenReturn(List.of(target));
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));

        ResolveDisplayNamesResponse response = userDirectoryService.resolveDisplayNames(
                "admin-uid",
                new ResolveDisplayNamesRequest(List.of("ouyzgpfoiahoih=="), 1L));

        assertFalse(response.success());
        assertEquals(1, response.requestedCount());
        assertEquals(0, response.resolvedCount());
        assertTrue(response.displayNames().isEmpty());
        assertEquals(List.of("ouyzgpfoiahoih=="), response.unresolvedUids());
        assertEquals("Schoolintegratie is niet ingeschakeld", response.message());
    }

    @Test
    void givenLiveUsersWithoutMatch_whenResolveDisplayNames_thenReturnsUnresolved() {
        SchoolEntity school = school(1L, "AP Hogeschool");

        UserEntity actor = user(1L, "admin-uid", UserRoles.ADMIN, school);
        UserEntity target = user(2L, "ouyzgpfoiahoih==", UserRoles.STUDENT, school);

        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();
        integration.setSchool(school);
        integration.setOnerosterEnabled(true);

        Map<String, Object> unrelatedLiveUser = Map.of(
                "identifier", "someone.else",
                "givenName", "Someone",
                "familyName", "Else");

        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findAllBySchool_IdAndSmartschoolUidIn(
                1L,
                List.of("ouyzgpfoiahoih=="))).thenReturn(List.of(target));
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));
        when(authService.getAccessToken(integration)).thenReturn("token-123");
        when(oneRosterClient.getUsers(integration, "token-123")).thenReturn(List.of(unrelatedLiveUser));

        ResolveDisplayNamesResponse response = userDirectoryService.resolveDisplayNames(
                "admin-uid",
                new ResolveDisplayNamesRequest(List.of("ouyzgpfoiahoih=="), 1L));

        assertTrue(response.success());
        assertEquals(1, response.requestedCount());
        assertEquals(0, response.resolvedCount());
        assertTrue(response.displayNames().isEmpty());
        assertEquals(List.of("ouyzgpfoiahoih=="), response.unresolvedUids());
        assertEquals("Geen display names gevonden", response.message());
    }

    private SchoolEntity school(Long id, String name) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        school.setName(name);
        school.setDomain("ap.test");
        return school;
    }

    private UserEntity user(Long id, String uid, UserRoles role, SchoolEntity school) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(uid);
        user.setRole(role);
        user.setSchool(school);
        return user;
    }
}