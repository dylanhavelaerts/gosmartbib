package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    void givenBibbeheerderExists_whenListUsersForAdmin_thenReturnsActiveMappedUsersFromSameSchool() {
        UserEntity actor = buildUser(1L, "admin-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School", true);
        UserEntity student = buildUser(4L, "student-uid", UserRoles.STUDENT, 100L, "GO! School", true);
        UserEntity teacher = buildUser(5L, "teacher-uid", UserRoles.TEACHER, 100L, "GO! School", true);
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> userPage = new PageImpl<>(List.of(student, teacher));

        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        // Aangepast naar findBySchoolIdAndName zoals gedefinieerd in de Service
        when(userRepository.findBySchoolIdAndName(100L, null, pageable)).thenReturn(userPage);

        Page<AdminUserDTO> result = userAdminService.listUsersForAdmin("admin-uid", null, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("student-uid", result.getContent().get(0).smartschoolUid());
        assertEquals(UserRoles.STUDENT, result.getContent().get(0).role());
        assertEquals("GO! School", result.getContent().get(0).school().name());
        assertEquals(1, result.getContent().get(0).classes().size());

        assertEquals("teacher-uid", result.getContent().get(1).smartschoolUid());
        assertEquals(UserRoles.TEACHER, result.getContent().get(1).role());

        verify(userRepository).findDetailedBySmartschoolUid("admin-uid");
        verify(userRepository).findBySchoolIdAndName(100L, null, pageable);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void givenActorDoesNotExist_whenListUsersForAdmin_thenThrowsNotFound() {
        when(userRepository.findDetailedBySmartschoolUid("missing-admin")).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminService.listUsersForAdmin("missing-admin", null, pageable));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Ingelogde gebruiker niet gevonden", exception.getReason());
        verify(userRepository).findDetailedBySmartschoolUid("missing-admin");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void givenActorIsNotBibbeheerder_whenListUsersForAdmin_thenThrowsForbidden() {
        UserEntity actor = buildUser(1L, "teacher-uid", UserRoles.TEACHER, 100L, "GO! School", true);
        when(userRepository.findDetailedBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(actor));
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminService.listUsersForAdmin("teacher-uid", null, pageable));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Geen toegang", exception.getReason());
        verify(userRepository).findDetailedBySmartschoolUid("teacher-uid");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void givenNullRole_whenUpdateUserRole_thenThrowsBadRequestWithoutRepositoryCalls() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminService.updateUserRole("admin-uid", 2L, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Nieuwe rol ontbreekt", exception.getReason());
        verifyNoInteractions(userRepository);
    }

    @Test
    void givenTargetUserDoesNotExistInAdminsSchool_whenUpdateUserRole_thenThrowsNotFound() {
        UserEntity actor = buildUser(1L, "admin-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School", true);
        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findByIdAndSchool_Id(99L, 100L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminService.updateUserRole("admin-uid", 99L, UserRoles.TEACHER));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Gebruiker niet gevonden", exception.getReason());
        verify(userRepository).findDetailedBySmartschoolUid("admin-uid");
        verify(userRepository).findByIdAndSchool_Id(99L, 100L);
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void givenActorTriesToUpdateOwnRole_whenUpdateUserRole_thenThrowsBadRequest() {
        UserEntity actor = buildUser(1L, "admin-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School", true);
        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findByIdAndSchool_Id(1L, 100L)).thenReturn(Optional.of(actor));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userAdminService.updateUserRole("admin-uid", 1L, UserRoles.TEACHER));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Je kan je eigen rol niet aanpassen", exception.getReason());
        verify(userRepository).findDetailedBySmartschoolUid("admin-uid");
        verify(userRepository).findByIdAndSchool_Id(1L, 100L);
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void givenValidAdminAndTarget_whenUpdateUserRole_thenUpdatesRoleAndReturnsMappedDto() {
        UserEntity actor = buildUser(1L, "admin-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School", true);
        UserEntity target = buildUser(2L, "student-uid", UserRoles.STUDENT, 100L, "GO! School", true);

        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));
        when(userRepository.findByIdAndSchool_Id(2L, 100L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        AdminUserDTO result = userAdminService.updateUserRole("admin-uid", 2L, UserRoles.TEACHER);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("student-uid", result.smartschoolUid());
        assertEquals(UserRoles.TEACHER, result.role());
        assertEquals("GO! School", result.school().name());
        assertEquals(1, result.classes().size());
        assertEquals(UserRoles.TEACHER, target.getRole());

        verify(userRepository).findDetailedBySmartschoolUid("admin-uid");
        verify(userRepository).findByIdAndSchool_Id(2L, 100L);
        verify(userRepository).save(target);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void givenBibbeheerderExists_whenGetCurrentAdmin_thenReturnsActor() {
        UserEntity actor = buildUser(1L, "admin-uid", UserRoles.BIBLIOTHEEKBEHEERDER, 100L, "GO! School", true);
        when(userRepository.findDetailedBySmartschoolUid("admin-uid")).thenReturn(Optional.of(actor));

        UserEntity result = userAdminService.getCurrentAdmin("admin-uid");

        assertSame(actor, result);
        verify(userRepository).findDetailedBySmartschoolUid("admin-uid");
        verifyNoMoreInteractions(userRepository);
    }

    // Hulpmethodes
    private UserEntity buildUser(Long id, String uid, UserRoles role, Long schoolId, String schoolName,
            boolean active) {
        SchoolEntity school = new SchoolEntity();
        school.setId(schoolId);
        school.setName(schoolName);
        school.setDomain("school.example.be");

        SchoolClassEntity schoolClass = new SchoolClassEntity();
        schoolClass.setId(50L + id);
        schoolClass.setSchool(school);
        schoolClass.setName("1A");
        schoolClass.setGrade("1");
        schoolClass.setSchoolYear("2025-2026");
        schoolClass.setSmartschoolGroupId("group-" + id);

        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(uid);
        user.setRole(role);
        user.setActive(active);
        user.setSchool(school);
        user.setClasses(Set.of(schoolClass));
        return user;
    }
}