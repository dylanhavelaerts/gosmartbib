package edu.ap.gosmartlib.services.schoolintegration;

import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationDTO;
import edu.ap.gosmartlib.dto.schoolintegration.UpsertSchoolIntegrationRequest;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.repositories.school.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import edu.ap.gosmartlib.services.oneroster.OneRosterSyncService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolIntegrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private SchoolIntegrationRepository schoolIntegrationRepository;
    @Mock private OneRosterSyncService oneRosterSyncService;

    @InjectMocks private SchoolIntegrationService schoolIntegrationService;

    private static final String LIBRARIAN_UID = "librarian-uid";
    private static final Long SCHOOL_ID = 1L;

    private UserEntity librarian;
    private SchoolEntity school;
    private SchoolIntegrationEntity integrationEntity;

    @BeforeEach
    void setUp() {
        school = new SchoolEntity();
        school.setId(SCHOOL_ID);
        school.setName("Test School");
        school.setDomain("school.smartschool.be");

        librarian = new UserEntity();
        librarian.setSmartschoolUid(LIBRARIAN_UID);
        librarian.setRole(UserRoles.LIBRARIAN);
        librarian.setSchool(school);

        integrationEntity = new SchoolIntegrationEntity();
        integrationEntity.setId(10L);
        integrationEntity.setSchool(school);
        integrationEntity.setSchoolBaseUrl("https://school.smartschool.be");
        integrationEntity.setOnerosterClientId("client-id");
        integrationEntity.setOnerosterClientSecret("secret");
        integrationEntity.setOnerosterEnabled(false);
    }

    // --- getIntegration ---

    @Test
    void givenLibrarianAndExistingIntegration_whenGetIntegration_thenReturnsMappedDTO() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(integrationEntity));

        SchoolIntegrationDTO result = schoolIntegrationService.getIntegration(LIBRARIAN_UID, SCHOOL_ID);

        assertEquals(SCHOOL_ID, result.schoolId());
        assertEquals("client-id", result.onerosterClientId());
        assertFalse(result.onerosterEnabled());
    }

    @Test
    void givenLibrarianButNoIntegration_whenGetIntegration_thenThrows404() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.getIntegration(LIBRARIAN_UID, SCHOOL_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- getCurrentLibrarian ---

    @Test
    void givenUserNotFound_whenGetCurrentLibrarian_thenThrows404() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.getCurrentLibrarian(LIBRARIAN_UID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenUserIsStudent_whenGetCurrentLibrarian_thenThrows403() {
        librarian.setRole(UserRoles.STUDENT);
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.getCurrentLibrarian(LIBRARIAN_UID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- getIntegrationEntityForLibrarian ---

    @Test
    void givenLibrarianAndExistingIntegration_whenGetIntegrationEntityForLibrarian_thenReturnsEntity() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(integrationEntity));

        SchoolIntegrationEntity result = schoolIntegrationService.getIntegrationEntityForLibrarian(LIBRARIAN_UID, SCHOOL_ID);

        assertEquals(integrationEntity, result);
    }

    @Test
    void givenLibrarianButMissingIntegration_whenGetIntegrationEntityForLibrarian_thenThrows404() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.getIntegrationEntityForLibrarian(LIBRARIAN_UID, SCHOOL_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- upsertIntegration ---

    @Test
    void givenNullRequest_whenUpsertIntegration_thenThrows400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void givenBlankBaseUrl_whenUpsertIntegration_thenThrows400() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest("  ", "client-id", "secret", false, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void givenBlankClientId_whenUpsertIntegration_thenThrows400() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest("https://school.be", "  ", "secret", false, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void givenNoExistingIntegrationAndNoSecret_whenUpsertIntegration_thenThrows400() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be", "client-id", "", false, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void givenSchoolNotFound_whenUpsertIntegration_thenThrows404() {
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.empty());
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be", "client-id", "secret", false, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenNewIntegrationWithOnerosterDisabled_whenUpsertIntegration_thenSavesWithoutSyncing() {
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be/", "client-id", "secret", false, null);
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolIntegrationDTO result = schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request);

        assertEquals("https://school.be", result.schoolBaseUrl());
        assertFalse(result.onerosterEnabled());
        verify(oneRosterSyncService, never()).syncSchool(any());
    }

    @Test
    void givenNewIntegrationWithOnerosterEnabled_whenUpsertIntegration_thenSavesAndSyncs() {
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be", "client-id", "secret", true, null);
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request);

        verify(oneRosterSyncService).syncSchool(any());
    }

    @Test
    void givenExistingIntegrationWithBlankSecret_whenUpsertIntegration_thenKeepsExistingSecret() {
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be", "new-client-id", "  ", false, null);
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(integrationEntity));
        when(schoolIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolIntegrationDTO result = schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request);

        assertEquals("new-client-id", result.onerosterClientId());
        assertTrue(result.clientSecretConfigured());
    }

    @Test
    void givenBaseUrlWithTrailingSlash_whenUpsertIntegration_thenStripsTrailingSlash() {
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be/api/", "client-id", "secret", false, null);
        when(userRepository.findDetailedBySmartschoolUid(LIBRARIAN_UID)).thenReturn(Optional.of(librarian));
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolIntegrationDTO result = schoolIntegrationService.upsertIntegration(LIBRARIAN_UID, SCHOOL_ID, request);

        assertEquals("https://school.be/api", result.schoolBaseUrl());
    }

    // --- getIntegrationForPlatformAdmin ---

    @Test
    void givenExistingIntegration_whenGetIntegrationForPlatformAdmin_thenReturnsMappedDTO() {
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(integrationEntity));

        SchoolIntegrationDTO result = schoolIntegrationService.getIntegrationForPlatformAdmin(SCHOOL_ID);

        assertEquals(SCHOOL_ID, result.schoolId());
    }

    @Test
    void givenNoIntegration_whenGetIntegrationForPlatformAdmin_thenThrows404() {
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.getIntegrationForPlatformAdmin(SCHOOL_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- upsertIntegrationForPlatformAdmin ---

    @Test
    void givenNullRequest_whenUpsertIntegrationForPlatformAdmin_thenThrows400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.upsertIntegrationForPlatformAdmin(SCHOOL_ID, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void givenValidRequest_whenUpsertIntegrationForPlatformAdmin_thenCreatesIntegration() {
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be", "client-id", "secret", false, "accesscode");
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolIntegrationDTO result = schoolIntegrationService.upsertIntegrationForPlatformAdmin(SCHOOL_ID, request);

        assertEquals("client-id", result.onerosterClientId());
        assertTrue(result.smartschoolAccesscodeConfigured());

        ArgumentCaptor<SchoolIntegrationEntity> captor = ArgumentCaptor.forClass(SchoolIntegrationEntity.class);
        verify(schoolIntegrationRepository).save(captor.capture());
        assertEquals(school, captor.getValue().getSchool());
    }

    @Test
    void givenEnabledOneroster_whenUpsertIntegrationForPlatformAdmin_thenSyncs() {
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be", "client-id", "secret", true, null);
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        schoolIntegrationService.upsertIntegrationForPlatformAdmin(SCHOOL_ID, request);

        verify(oneRosterSyncService).syncSchool(any());
    }

    @Test
    void givenSchoolNotFound_whenUpsertIntegrationForPlatformAdmin_thenThrows404() {
        UpsertSchoolIntegrationRequest request = new UpsertSchoolIntegrationRequest(
                "https://school.be", "client-id", "secret", false, null);
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.upsertIntegrationForPlatformAdmin(SCHOOL_ID, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- getIntegrationEntityForPlatformAdmin ---

    @Test
    void givenExistingIntegration_whenGetIntegrationEntityForPlatformAdmin_thenReturnsEntity() {
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(integrationEntity));

        SchoolIntegrationEntity result = schoolIntegrationService.getIntegrationEntityForPlatformAdmin(SCHOOL_ID);

        assertEquals(integrationEntity, result);
    }

    @Test
    void givenNoIntegration_whenGetIntegrationEntityForPlatformAdmin_thenThrows404() {
        when(schoolIntegrationRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolIntegrationService.getIntegrationEntityForPlatformAdmin(SCHOOL_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
