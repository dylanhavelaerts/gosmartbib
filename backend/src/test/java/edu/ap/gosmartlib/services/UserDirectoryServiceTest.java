package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO;
<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceTest {

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
    void givenLiveUserWithLegacyIdentifier_whenSearchUsersForLoan_thenReturnsResolvedLiveName() {
        SchoolEntity school = school(1L, "AP Hogeschool");
        UserEntity actor = user(10L, "admin-uid", UserRoles.ADMIN, school, Set.of());
        UserEntity borrower = user(
                11L,
                "piuogheziugsoqihf=",
                UserRoles.STUDENT,
                school,
                Set.of(schoolClass(school, "2ITSOF")));

        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();
        integration.setSchool(school);
        integration.setOnerosterEnabled(true);

        Map<String, Object> liveUser = Map.of(
                "identifier", "sof2.first.last",
                "username", "sof2.first.last",
                "givenName", "First",
                "familyName", "Last",
                "metadata", Map.of("smsc.legacyIdentifier", "piuogheziugsoqihf="));

        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(1L))
                .thenReturn(List.of(borrower));
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));
        when(authService.getAccessToken(integration)).thenReturn("token-123");
        when(oneRosterClient.getUsers(integration, "token-123")).thenReturn(List.of(liveUser));

        List<SmartschoolUserDTO> result = userDirectoryService.searchUsersForLoan("admin-uid", "first");

        assertEquals(1, result.size());
        SmartschoolUserDTO dto = result.get(0);

        assertEquals("piuogheziugsoqihf=", dto.smartschoolUserId());
        assertEquals("First Last", dto.name());
        assertEquals("2ITSOF", dto.classGroup());
        assertEquals("AP Hogeschool", dto.school());
        assertEquals("1", dto.schoolId());
        assertTrue(dto.photoUrl().contains("ui-avatars.com"));
    }

    @Test
    void givenNoLiveMatch_whenSearchUsersForLoan_thenFallsBackToUidSearch() {
        SchoolEntity school = school(1L, "AP Hogeschool");
        UserEntity actor = user(10L, "admin-uid", UserRoles.ADMIN, school, Set.of());
        UserEntity borrower = user(
                11L,
                "piuogheziugsoqihf=",
                UserRoles.STUDENT,
                school,
                Set.of(schoolClass(school, "2ITSOF")));

        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();
        integration.setSchool(school);
        integration.setOnerosterEnabled(true);

        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(1L))
                .thenReturn(List.of(borrower));
        when(schoolIntegrationRepository.findBySchool_Id(1L)).thenReturn(Optional.of(integration));
        when(authService.getAccessToken(integration)).thenReturn("token-123");
        when(oneRosterClient.getUsers(integration, "token-123")).thenReturn(List.of());

        List<SmartschoolUserDTO> result = userDirectoryService.searchUsersForLoan("admin-uid", "piuo");

        assertEquals(1, result.size());
        assertEquals("piuogheziugsoqihf=", result.get(0).name());
    }

    private SchoolEntity school(Long id, String name) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        school.setName(name);
        school.setDomain("ap.test");
        return school;
    }

    private SchoolClassEntity schoolClass(SchoolEntity school, String name) {
        SchoolClassEntity schoolClass = new SchoolClassEntity();
        schoolClass.setSchool(school);
        schoolClass.setName(name);
        return schoolClass;
    }

    private UserEntity user(Long id, String uid, UserRoles role, SchoolEntity school, Set<SchoolClassEntity> classes) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(uid);
        user.setRole(role);
        user.setSchool(school);
        user.setClasses(classes);
        return user;
    }
}