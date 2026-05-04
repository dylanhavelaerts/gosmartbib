package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.CreateSchoolCampusRequest;
import edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus.SchoolCampusDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.schoolIntegration.schoolCampus.SchoolCampusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolCampusControllerTest {

    @Mock
    private SchoolCampusService schoolCampusService;

    @Mock
    private OAuth2User oAuth2User;

    // @Spy gebruikt de echte implementatie van AuthHelper zodat extractUid/extractUidOrNull
    // correct werken zonder elke test afzonderlijk te stubben.
    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private SchoolCampusController schoolCampusController;

    @Test
    void givenValidOAuthUser_whenGetCampuses_thenDelegatesWithExtractedUidAndSchoolId() {
        List<SchoolCampusDTO> expected = List.of(
                new SchoolCampusDTO(1L, "Campus Noord"),
                new SchoolCampusDTO(2L, "Campus Zuid"));

        when(oAuth2User.getAttribute("userID")).thenReturn("admin-uid");
        when(schoolCampusService.getCampusesForAdminSchool("admin-uid", 100L)).thenReturn(expected);

        List<SchoolCampusDTO> result = schoolCampusController.getCampuses(100L, oAuth2User);

        assertEquals(expected, result);

        verify(oAuth2User).getAttribute("userID");
        verify(schoolCampusService).getCampusesForAdminSchool("admin-uid", 100L);
        verifyNoMoreInteractions(oAuth2User, schoolCampusService);
    }

    @Test
    void givenValidOAuthUserAndRequest_whenCreateCampus_thenDelegatesWithExtractedUidAndSchoolId() {
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Zuid");
        SchoolCampusDTO expected = new SchoolCampusDTO(3L, "Campus Zuid");

        when(oAuth2User.getAttribute("userID")).thenReturn("admin-uid");
        when(schoolCampusService.createCampusForAdminSchool("admin-uid", 100L, request)).thenReturn(expected);

        SchoolCampusDTO result = schoolCampusController.createCampus(100L, request, oAuth2User);

        assertEquals(expected, result);

        verify(oAuth2User).getAttribute("userID");
        verify(schoolCampusService).createCampusForAdminSchool("admin-uid", 100L, request);
        verifyNoMoreInteractions(oAuth2User, schoolCampusService);
    }

    @Test
    void givenValidOAuthUser_whenDeleteCampus_thenDelegatesWithExtractedUidSchoolIdAndCampusId() {
        when(oAuth2User.getAttribute("userID")).thenReturn("admin-uid");

        schoolCampusController.deleteCampus(100L, 5L, oAuth2User);

        verify(oAuth2User).getAttribute("userID");
        verify(schoolCampusService).deleteCampusForAdminSchool("admin-uid", 100L, 5L);
        verifyNoMoreInteractions(oAuth2User, schoolCampusService);
    }

    @Test
    void givenNullOAuthUser_whenGetCampuses_thenThrowsUnauthorized() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusController.getCampuses(100L, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Niet ingelogd", exception.getReason());

        verifyNoInteractions(schoolCampusService);
    }

    @Test
    void givenMissingUid_whenGetCampuses_thenThrowsUnauthorized() {
        when(oAuth2User.getAttribute("userID")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusController.getCampuses(100L, oAuth2User));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());

        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(schoolCampusService);
    }

    @Test
    void givenBlankUid_whenCreateCampus_thenThrowsUnauthorized() {
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Zuid");
        when(oAuth2User.getAttribute("userID")).thenReturn("   ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusController.createCampus(100L, request, oAuth2User));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());

        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(schoolCampusService);
    }
}