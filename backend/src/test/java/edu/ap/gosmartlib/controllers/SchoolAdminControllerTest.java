package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.controllers.school.SchoolAdminController;
import edu.ap.gosmartlib.dto.school.ApproveSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolResult;
import edu.ap.gosmartlib.dto.school.SchoolDTO;
import edu.ap.gosmartlib.services.school.SchoolAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolAdminControllerTest {

    @Mock
    private SchoolAdminService schoolAdminService;

    @InjectMocks
    private SchoolAdminController schoolAdminController;

    @Test
    void givenSchoolsExist_whenListSchools_thenReturnsDelegatedList() {
        List<SchoolDTO> expected = List.of(
                new SchoolDTO(1L, "GO! Atheneum", "https://go.smartschool.be", true),
                new SchoolDTO(2L, "https://pending.smartschool.be", "https://pending.smartschool.be", false));

        when(schoolAdminService.listAllSchools()).thenReturn(expected);

        List<SchoolDTO> result = schoolAdminController.listSchools();

        assertEquals(expected, result);
        verify(schoolAdminService).listAllSchools();
        verifyNoMoreInteractions(schoolAdminService);
    }

    @Test
    void givenNewDomain_whenCreateSchool_thenReturns201WithBody() {
        CreateSchoolRequest request = new CreateSchoolRequest("GO! Atheneum", "https://go.smartschool.be");
        SchoolDTO dto = new SchoolDTO(1L, "GO! Atheneum", "https://go.smartschool.be", true);

        when(schoolAdminService.createSchool(request)).thenReturn(new CreateSchoolResult(dto, false));

        ResponseEntity<SchoolDTO> response = schoolAdminController.createSchool(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(dto, response.getBody());
        verify(schoolAdminService).createSchool(request);
    }

    @Test
    void givenExistingDomain_whenCreateSchool_thenReturns409WithExistingSchoolBody() {
        CreateSchoolRequest request = new CreateSchoolRequest("GO! Atheneum", "https://go.smartschool.be");
        SchoolDTO existing = new SchoolDTO(5L, "GO! Atheneum", "https://go.smartschool.be", true);

        when(schoolAdminService.createSchool(request)).thenReturn(new CreateSchoolResult(existing, true));

        ResponseEntity<SchoolDTO> response = schoolAdminController.createSchool(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(5L, response.getBody().id());
        verify(schoolAdminService).createSchool(request);
    }

    @Test
    void givenInvalidRequest_whenCreateSchool_thenPropagatesServiceException() {
        CreateSchoolRequest request = new CreateSchoolRequest("", "https://go.smartschool.be");

        when(schoolAdminService.createSchool(request))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schoolnaam is verplicht"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminController.createSchool(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(schoolAdminService).createSchool(request);
    }

    @Test
    void givenValidIdAndName_whenApproveSchool_thenReturnsDelegatedDTO() {
        ApproveSchoolRequest request = new ApproveSchoolRequest("Echte Naam");
        SchoolDTO approved = new SchoolDTO(3L, "Echte Naam", "https://pending.smartschool.be", true);

        when(schoolAdminService.approveSchool(3L, request)).thenReturn(approved);

        SchoolDTO result = schoolAdminController.approveSchool(3L, request);

        assertEquals(approved, result);
        verify(schoolAdminService).approveSchool(3L, request);
        verifyNoMoreInteractions(schoolAdminService);
    }

    @Test
    void givenUnknownId_whenApproveSchool_thenPropagatesServiceException() {
        ApproveSchoolRequest request = new ApproveSchoolRequest("Naam");

        when(schoolAdminService.approveSchool(99L, request))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminController.approveSchool(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenValidId_whenDeleteSchool_thenDelegatesAndReturnsNoContent() {
        doNothing().when(schoolAdminService).deleteSchool(10L);

        schoolAdminController.deleteSchool(10L);

        verify(schoolAdminService).deleteSchool(10L);
        verifyNoMoreInteractions(schoolAdminService);
    }

    @Test
    void givenUnknownId_whenDeleteSchool_thenPropagatesServiceException() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden"))
                .when(schoolAdminService).deleteSchool(99L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolAdminController.deleteSchool(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenControllerMethods_whenCheckingPreAuthorize_thenAllRequireIsAdmin() throws NoSuchMethodException {
        Class<SchoolAdminController> cls = SchoolAdminController.class;

        assertPreAuthorizeIsAdmin(cls.getMethod("listSchools"));
        assertPreAuthorizeIsAdmin(cls.getMethod("createSchool", CreateSchoolRequest.class));
        assertPreAuthorizeIsAdmin(cls.getMethod("approveSchool", Long.class, ApproveSchoolRequest.class));
        assertPreAuthorizeIsAdmin(cls.getMethod("deleteSchool", Long.class));
    }

    private void assertPreAuthorizeIsAdmin(java.lang.reflect.Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "Missing @PreAuthorize on " + method.getName());
        assertEquals("@roleGuard.isAdmin(authentication)", annotation.value());
    }
}