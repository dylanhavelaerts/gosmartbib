package edu.ap.gosmartlib.services.school;

import edu.ap.gosmartlib.dto.school.ApproveSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolResult;
import edu.ap.gosmartlib.dto.school.SchoolDTO;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.*;
import edu.ap.gosmartlib.services.users.UserDeletionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolAdminServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeletionService userDeletionService;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SchoolIntegrationRepository schoolIntegrationRepository;

    @InjectMocks
    private SchoolAdminService schoolAdminService;

    @Mock
    private BookInventoryRepository bookInventoryRepository;

    // ── listAllSchools ────────────────────────────────────────────────────────

    @Test
    void givenSchoolsExist_whenListAllSchools_thenReturnsMappedDTOs() {
        SchoolEntity approved = buildSchool(1L, "GO! Atheneum", "https://go.smartschool.be", true);
        SchoolEntity pending = buildSchool(2L, "https://pending.smartschool.be", "https://pending.smartschool.be", false);

        when(schoolRepository.findAllByOrderByAdminApprovedAscNameAsc()).thenReturn(List.of(pending, approved));

        List<SchoolDTO> result = schoolAdminService.listAllSchools();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).id());
        assertFalse(result.get(0).adminApproved());
        assertEquals(1L, result.get(1).id());
        assertTrue(result.get(1).adminApproved());

        verify(schoolRepository).findAllByOrderByAdminApprovedAscNameAsc();
        verifyNoMoreInteractions(schoolRepository);
    }

    @Test
    void givenNoSchools_whenListAllSchools_thenReturnsEmptyList() {
        when(schoolRepository.findAllByOrderByAdminApprovedAscNameAsc()).thenReturn(List.of());

        List<SchoolDTO> result = schoolAdminService.listAllSchools();

        assertTrue(result.isEmpty());
        verify(schoolRepository).findAllByOrderByAdminApprovedAscNameAsc();
    }

    @Test
    void givenNewDomain_whenCreateSchool_thenSavesAndReturnsNotExisted() {
        CreateSchoolRequest request = new CreateSchoolRequest("GO! Atheneum", "https://go.smartschool.be");
        SchoolEntity saved = buildSchool(1L, "GO! Atheneum", "https://go.smartschool.be", true);

        when(schoolRepository.findByDomain("https://go.smartschool.be")).thenReturn(Optional.empty());
        when(schoolRepository.save(any(SchoolEntity.class))).thenReturn(saved);

        CreateSchoolResult result = schoolAdminService.createSchool(request);

        assertFalse(result.alreadyExisted());
        assertEquals(1L, result.school().id());
        assertEquals("GO! Atheneum", result.school().name());
        assertTrue(result.school().adminApproved());

        verify(schoolRepository).findByDomain("https://go.smartschool.be");
        verify(schoolRepository).save(any(SchoolEntity.class));
        verifyNoMoreInteractions(schoolRepository);
    }

    @Test
    void givenExistingDomain_whenCreateSchool_thenReturnsExistingSchoolWithAlreadyExistedTrue() {
        CreateSchoolRequest request = new CreateSchoolRequest("GO! Atheneum", "https://go.smartschool.be");
        SchoolEntity existing = buildSchool(5L, "GO! Atheneum", "https://go.smartschool.be", true);

        when(schoolRepository.findByDomain("https://go.smartschool.be")).thenReturn(Optional.of(existing));

        CreateSchoolResult result = schoolAdminService.createSchool(request);

        assertTrue(result.alreadyExisted());
        assertEquals(5L, result.school().id());

        verify(schoolRepository).findByDomain("https://go.smartschool.be");
        verify(schoolRepository, never()).save(any());
    }

    @Test
    void givenBlankName_whenCreateSchool_thenThrowsBadRequest() {
        CreateSchoolRequest request = new CreateSchoolRequest("  ", "https://go.smartschool.be");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminService.createSchool(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Schoolnaam is verplicht", ex.getReason());
        verifyNoInteractions(schoolRepository);
    }

    @Test
    void givenBlankDomain_whenCreateSchool_thenThrowsBadRequest() {
        CreateSchoolRequest request = new CreateSchoolRequest("GO! Atheneum", "   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminService.createSchool(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Schooldomein is verplicht", ex.getReason());
        verifyNoInteractions(schoolRepository);
    }

    @Test
    void givenNullName_whenCreateSchool_thenThrowsBadRequest() {
        CreateSchoolRequest request = new CreateSchoolRequest(null, "https://go.smartschool.be");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminService.createSchool(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void givenPendingSchool_whenApproveSchool_thenSetsApprovedAndUpdatesName() {
        SchoolEntity pending = buildSchool(3L, "https://pending.smartschool.be", "https://pending.smartschool.be", false);
        SchoolEntity approved = buildSchool(3L, "Echte Schoolnaam", "https://pending.smartschool.be", true);

        when(schoolRepository.findById(3L)).thenReturn(Optional.of(pending));
        when(schoolRepository.save(pending)).thenReturn(approved);

        SchoolDTO result = schoolAdminService.approveSchool(3L, new ApproveSchoolRequest("Echte Schoolnaam"));

        assertTrue(result.adminApproved());
        assertEquals("Echte Schoolnaam", result.name());

        verify(schoolRepository).findById(3L);
        verify(schoolRepository).save(pending);
    }

    @Test
    void givenUnknownId_whenApproveSchool_thenThrowsNotFound() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminService.approveSchool(99L, new ApproveSchoolRequest("Naam")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(schoolRepository).findById(99L);
        verify(schoolRepository, never()).save(any());
    }

    @Test
    void givenBlankName_whenApproveSchool_thenThrowsBadRequest() {
        SchoolEntity pending = buildSchool(3L, "pending.smartschool.be", "https://pending.smartschool.be", false);
        when(schoolRepository.findById(3L)).thenReturn(Optional.of(pending));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminService.approveSchool(3L, new ApproveSchoolRequest("  ")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(schoolRepository, never()).save(any());
    }

    @Test
    void givenSchoolWithUsersClassesAndIntegration_whenDeleteSchool_thenDeletesInOrder() {
        SchoolEntity school = buildSchool(10L, "GO! Atheneum", "https://go.smartschool.be", true);
        UserEntity user1 = new UserEntity();
        user1.setSmartschoolUid("uid-1");
        UserEntity user2 = new UserEntity();
        user2.setSmartschoolUid("uid-2");
        SchoolClassEntity cls = new SchoolClassEntity();
        SchoolIntegrationEntity integration = new SchoolIntegrationEntity();

        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(10L)).thenReturn(List.of(user1, user2));
        when(schoolClassRepository.findAllBySchool_IdOrderByNameAsc(10L)).thenReturn(List.of(cls));
        when(schoolIntegrationRepository.findBySchool_Id(10L)).thenReturn(Optional.of(integration));

        schoolAdminService.deleteSchool(10L);

        verify(userDeletionService).deleteUser(user1);
        verify(userDeletionService).deleteUser(user2);
        verify(schoolClassRepository).deleteAll(List.of(cls));
        verify(schoolIntegrationRepository).delete(integration);
        verify(bookInventoryRepository).deleteAllBySchool_Id(10L);
        verify(schoolRepository).delete(school);
    }

    @Test
    void givenSchoolWithNoUsersOrIntegration_whenDeleteSchool_thenSkipsThoseSteps() {
        SchoolEntity school = buildSchool(11L, "Leeg", "https://leeg.smartschool.be", true);

        when(schoolRepository.findById(11L)).thenReturn(Optional.of(school));
        when(userRepository.findAllBySchool_IdOrderBySmartschoolUidAsc(11L)).thenReturn(List.of());
        when(schoolClassRepository.findAllBySchool_IdOrderByNameAsc(11L)).thenReturn(List.of());
        when(schoolIntegrationRepository.findBySchool_Id(11L)).thenReturn(Optional.empty());

        schoolAdminService.deleteSchool(11L);

        verifyNoInteractions(userDeletionService);
        verify(schoolClassRepository).deleteAll(List.of());
        verify(schoolIntegrationRepository, never()).delete(any());
        verify(bookInventoryRepository).deleteAllBySchool_Id(11L);
        verify(schoolRepository).delete(school);
    }

    @Test
    void givenUnknownId_whenDeleteSchool_thenThrowsNotFound() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminService.deleteSchool(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(userDeletionService, schoolClassRepository, schoolIntegrationRepository, bookInventoryRepository);
        verify(schoolRepository, never()).delete(any());
    }

    private SchoolEntity buildSchool(Long id, String name, String domain, boolean adminApproved) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        school.setName(name);
        school.setDomain(domain);
        school.setAdminApproved(adminApproved);
        return school;
    }
}