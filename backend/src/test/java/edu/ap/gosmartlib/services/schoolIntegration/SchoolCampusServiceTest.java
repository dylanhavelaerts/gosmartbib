package edu.ap.gosmartlib.services.schoolIntegration;

import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.CreateSchoolCampusRequest;
import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.SchoolCampusDTO;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolCampusEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolCampusRepository;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.schoolIntegration.schoolCampus.SchoolCampusService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolCampusServiceTest {

    @Mock
    private SchoolCampusRepository schoolCampusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolCampusService schoolCampusService;

    @Test
    void givenBibbeheerderFromRequestedSchool_whenGetCampuses_thenReturnsCampusesForThatSchool() {
        UserEntity admin = buildUser("bibbeheerder-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School");

        List<SchoolCampusEntity> campuses = List.of(
                buildCampus(1L, 100L, "Campus Noord"),
                buildCampus(2L, 100L, "Campus Zuid"));

        when(userRepository.findDetailedBySmartschoolUid("bibbeheerder-uid")).thenReturn(Optional.of(admin));
        when(schoolCampusRepository.findBySchool_IdOrderByNameAsc(100L)).thenReturn(campuses);

        List<SchoolCampusDTO> result = schoolCampusService.getCampusesForBibbeheerder("bibbeheerder-uid", 100L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Campus Noord", result.get(0).name());
        assertEquals(2L, result.get(1).id());
        assertEquals("Campus Zuid", result.get(1).name());

        verify(userRepository).findDetailedBySmartschoolUid("bibbeheerder-uid");
        verify(schoolCampusRepository).findBySchool_IdOrderByNameAsc(100L);
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }

    @Test
    void givenBibbeheerderFromOtherSchool_whenGetCampuses_thenThrowsForbiddenAndDoesNotLoadCampuses() {
        UserEntity admin = buildUser("bibbeheerder-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School");

        when(userRepository.findDetailedBySmartschoolUid("bibbeheerder-uid")).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.getCampusesForBibbeheerder("bibbeheerder-uid", 200L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Geen toegang tot deze school", exception.getReason());

        verify(userRepository).findDetailedBySmartschoolUid("bibbeheerder-uid");
        verify(schoolCampusRepository, never()).findBySchool_IdOrderByNameAsc(any());
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }

    @Test
    void givenNonBibbeheerderActor_whenGetCampuses_thenThrowsForbidden() {
        UserEntity actor = buildUser("leerkracht-uid", UserRoles.TEACHER, 100L, "GO! School");

        when(userRepository.findDetailedBySmartschoolUid("leerkracht-uid")).thenReturn(Optional.of(actor));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.getCampusesForBibbeheerder("leerkracht-uid", 100L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Geen toegang", exception.getReason());

        verify(userRepository).findDetailedBySmartschoolUid("leerkracht-uid");
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }

    @Test
    void givenBibbeheerderAndNewCampusName_whenCreateCampus_thenNormalizesNameAndSavesForBibbeheerderSchool() {
        UserEntity admin = buildUser("bibbeheerder-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School");
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("  Campus   Zuid  ");

        when(userRepository.findDetailedBySmartschoolUid("bibbeheerder-uid")).thenReturn(Optional.of(admin));
        when(schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(100L, "Campus Zuid")).thenReturn(false);
        when(schoolCampusRepository.save(any(SchoolCampusEntity.class))).thenAnswer(invocation -> {
            SchoolCampusEntity campus = invocation.getArgument(0);
            campus.setId(10L);
            return campus;
        });

        SchoolCampusDTO result = schoolCampusService.createCampusForBibbeheerder("bibbeheerder-uid", 100L, request);

        assertEquals(10L, result.id());
        assertEquals("Campus Zuid", result.name());

        ArgumentCaptor<SchoolCampusEntity> captor = ArgumentCaptor.forClass(SchoolCampusEntity.class);
        verify(schoolCampusRepository).save(captor.capture());

        SchoolCampusEntity savedCampus = captor.getValue();

        assertSame(admin.getSchool(), savedCampus.getSchool());
        assertEquals("Campus Zuid", savedCampus.getName());

        verify(userRepository).findDetailedBySmartschoolUid("bibbeheerder-uid");
        verify(schoolCampusRepository).existsBySchool_IdAndNameIgnoreCase(100L, "Campus Zuid");
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }

    @Test
    void givenBlankCampusName_whenCreateCampus_thenThrowsBadRequest() {
        UserEntity admin = buildUser("bibbeheerder-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School");
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("   ");

        when(userRepository.findDetailedBySmartschoolUid("bibbeheerder-uid")).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.createCampusForBibbeheerder("bibbeheerder-uid", 100L, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Campusnaam is verplicht", exception.getReason());

        verify(userRepository).findDetailedBySmartschoolUid("bibbeheerder-uid");
        verify(schoolCampusRepository, never()).save(any(SchoolCampusEntity.class));
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }

    @Test
    void givenDuplicateCampusName_whenCreateCampus_thenThrowsConflict() {
        UserEntity admin = buildUser("bibbeheerder-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School");
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Zuid");

        when(userRepository.findDetailedBySmartschoolUid("bibbeheerder-uid")).thenReturn(Optional.of(admin));
        when(schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(100L, "Campus Zuid")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.createCampusForBibbeheerder("bibbeheerder-uid", 100L, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Campus bestaat al voor deze school", exception.getReason());

        verify(userRepository).findDetailedBySmartschoolUid("bibbeheerder-uid");
        verify(schoolCampusRepository).existsBySchool_IdAndNameIgnoreCase(100L, "Campus Zuid");
        verify(schoolCampusRepository, never()).save(any(SchoolCampusEntity.class));
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }

    @Test
    void givenExistingCampusInBibbeheerderSchool_whenDeleteCampus_thenDeletesCampus() {
        UserEntity admin = buildUser("bibbeheerder-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School");
        SchoolCampusEntity campus = buildCampus(5L, 100L, "Campus Zuid");

        when(userRepository.findDetailedBySmartschoolUid("bibbeheerder-uid")).thenReturn(Optional.of(admin));
        when(schoolCampusRepository.findByIdAndSchool_Id(5L, 100L)).thenReturn(Optional.of(campus));

        schoolCampusService.deleteCampusForBibbeheerder("bibbeheerder-uid", 100L, 5L);

        verify(userRepository).findDetailedBySmartschoolUid("bibbeheerder-uid");
        verify(schoolCampusRepository).findByIdAndSchool_Id(5L, 100L);
        verify(schoolCampusRepository).delete(campus);
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }

    @Test
    void givenCampusDoesNotExistInBibbeheerderSchool_whenDeleteCampus_thenThrowsNotFound() {
        UserEntity admin = buildUser("bibbeheerder-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School");

        when(userRepository.findDetailedBySmartschoolUid("bibbeheerder-uid")).thenReturn(Optional.of(admin));
        when(schoolCampusRepository.findByIdAndSchool_Id(99L, 100L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.deleteCampusForBibbeheerder("bibbeheerder-uid", 100L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Campus niet gevonden", exception.getReason());

        verify(userRepository).findDetailedBySmartschoolUid("bibbeheerder-uid");
        verify(schoolCampusRepository).findByIdAndSchool_Id(99L, 100L);
        verify(schoolCampusRepository, never()).delete(any(SchoolCampusEntity.class));
        verifyNoMoreInteractions(userRepository, schoolCampusRepository);
    }
    // ── ForPlatformAdmin ──────────────────────────────────────────────────────

    @Test
    void givenValidSchoolId_whenGetCampusesForPlatformAdmin_thenReturnsCampuses() {
        List<SchoolCampusEntity> campuses = List.of(
                buildCampus(1L, 100L, "Campus Noord"),
                buildCampus(2L, 100L, "Campus Zuid"));

        when(schoolCampusRepository.findBySchool_IdOrderByNameAsc(100L)).thenReturn(campuses);

        List<SchoolCampusDTO> result = schoolCampusService.getCampusesForPlatformAdmin(100L);

        assertEquals(2, result.size());
        assertEquals("Campus Noord", result.get(0).name());
        assertEquals("Campus Zuid", result.get(1).name());

        verify(schoolCampusRepository).findBySchool_IdOrderByNameAsc(100L);
        verifyNoMoreInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    @Test
    void givenNullSchoolId_whenGetCampusesForPlatformAdmin_thenThrowsBadRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.getCampusesForPlatformAdmin(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    @Test
    void givenValidSchoolAndName_whenCreateCampusForPlatformAdmin_thenSavesWithSchoolFromRepository() {
        SchoolEntity school = new SchoolEntity();
        school.setId(100L);
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("  Campus Noord  ");

        when(schoolRepository.findById(100L)).thenReturn(Optional.of(school));
        when(schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(100L, "Campus Noord")).thenReturn(false);
        when(schoolCampusRepository.save(any(SchoolCampusEntity.class))).thenAnswer(invocation -> {
            SchoolCampusEntity campus = invocation.getArgument(0);
            campus.setId(10L);
            return campus;
        });

        SchoolCampusDTO result = schoolCampusService.createCampusForPlatformAdmin(100L, request);

        assertEquals(10L, result.id());
        assertEquals("Campus Noord", result.name());

        ArgumentCaptor<SchoolCampusEntity> captor = ArgumentCaptor.forClass(SchoolCampusEntity.class);
        verify(schoolCampusRepository).save(captor.capture());
        assertSame(school, captor.getValue().getSchool());

        verify(schoolCampusRepository).existsBySchool_IdAndNameIgnoreCase(100L, "Campus Noord");
        verify(schoolRepository).findById(100L);
        verifyNoMoreInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    @Test
    void givenBlankName_whenCreateCampusForPlatformAdmin_thenThrowsBadRequest() {
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("   ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.createCampusForPlatformAdmin(100L, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Campusnaam is verplicht", exception.getReason());

        verify(schoolCampusRepository, never()).save(any());
        verifyNoMoreInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    @Test
    void givenDuplicateName_whenCreateCampusForPlatformAdmin_thenThrowsConflict() {
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Noord");

        when(schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(100L, "Campus Noord")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.createCampusForPlatformAdmin(100L, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Campus bestaat al voor deze school", exception.getReason());

        verify(schoolCampusRepository).existsBySchool_IdAndNameIgnoreCase(100L, "Campus Noord");
        verify(schoolCampusRepository, never()).save(any());
        verifyNoMoreInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    @Test
    void givenUnknownSchoolId_whenCreateCampusForPlatformAdmin_thenThrowsNotFound() {
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Noord");

        when(schoolCampusRepository.existsBySchool_IdAndNameIgnoreCase(99L, "Campus Noord")).thenReturn(false);
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.createCampusForPlatformAdmin(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("School niet gevonden", exception.getReason());

        verify(schoolCampusRepository, never()).save(any());
        verifyNoMoreInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    @Test
    void givenExistingCampus_whenDeleteCampusForPlatformAdmin_thenDeletesCampus() {
        SchoolCampusEntity campus = buildCampus(5L, 100L, "Campus Noord");

        when(schoolCampusRepository.findByIdAndSchool_Id(5L, 100L)).thenReturn(Optional.of(campus));

        schoolCampusService.deleteCampusForPlatformAdmin(100L, 5L);

        verify(schoolCampusRepository).findByIdAndSchool_Id(5L, 100L);
        verify(schoolCampusRepository).delete(campus);
        verifyNoMoreInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    @Test
    void givenNonExistentCampus_whenDeleteCampusForPlatformAdmin_thenThrowsNotFound() {
        when(schoolCampusRepository.findByIdAndSchool_Id(99L, 100L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusService.deleteCampusForPlatformAdmin(100L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Campus niet gevonden", exception.getReason());

        verify(schoolCampusRepository).findByIdAndSchool_Id(99L, 100L);
        verify(schoolCampusRepository, never()).delete(any());
        verifyNoMoreInteractions(schoolCampusRepository, schoolRepository, userRepository);
    }

    private UserEntity buildUser(String uid, UserRoles role, Long schoolId, String schoolName) {
        SchoolEntity school = new SchoolEntity();
        school.setId(schoolId);
        school.setName(schoolName);
        school.setDomain("school" + schoolId + ".example.be");

        UserEntity user = new UserEntity();
        user.setSmartschoolUid(uid);
        user.setRole(role);
        user.setSchool(school);

        return user;
    }

    private SchoolCampusEntity buildCampus(Long id, Long schoolId, String name) {
        SchoolEntity school = new SchoolEntity();
        school.setId(schoolId);
        school.setName("School " + schoolId);
        school.setDomain("school" + schoolId + ".example.be");

        SchoolCampusEntity campus = new SchoolCampusEntity();
        campus.setId(id);
        campus.setSchool(school);
        campus.setName(name);

        return campus;
    }
}
