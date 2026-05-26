package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.controllers.school.SchoolCampusController;
import edu.ap.gosmartlib.dto.schoolintegration.schoolCampus.CreateSchoolCampusRequest;
import edu.ap.gosmartlib.dto.schoolintegration.schoolCampus.SchoolCampusDTO;
import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.schoolintegration.schoolCampus.SchoolCampusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolCampusControllerTest {

    @Mock private SchoolCampusService schoolCampusService;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;

    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private SchoolCampusController schoolCampusController;

    @Test
    void givenValidOAuthUser_whenGetCampuses_thenDelegatesWithExtractedUidAndSchoolId() {
        List<SchoolCampusDTO> expected = List.of(
                new SchoolCampusDTO(1L, "Campus Noord"),
                new SchoolCampusDTO(2L, "Campus Zuid"));

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("bibbeheerder-uid");
        when(schoolCampusService.getCampusesForBibbeheerder("bibbeheerder-uid", 100L)).thenReturn(expected);

        List<SchoolCampusDTO> result = schoolCampusController.getCampuses(100L, authentication);

        assertEquals(expected, result);
        verify(authentication, times(2)).getPrincipal();
        verify(oAuth2User).getAttribute("userID");
        verify(schoolCampusService).getCampusesForBibbeheerder("bibbeheerder-uid", 100L);
        verifyNoMoreInteractions(authentication, oAuth2User, schoolCampusService);
    }

    @Test
    void givenValidOAuthUserAndRequest_whenCreateCampus_thenDelegatesWithExtractedUidAndSchoolId() {
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Zuid");
        SchoolCampusDTO expected = new SchoolCampusDTO(3L, "Campus Zuid");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("bibbeheerder-uid");
        when(schoolCampusService.createCampusForBibbeheerder("bibbeheerder-uid", 100L, request)).thenReturn(expected);

        SchoolCampusDTO result = schoolCampusController.createCampus(100L, request, authentication);

        assertEquals(expected, result);
        verify(authentication, times(2)).getPrincipal();
        verify(oAuth2User).getAttribute("userID");
        verify(schoolCampusService).createCampusForBibbeheerder("bibbeheerder-uid", 100L, request);
        verifyNoMoreInteractions(authentication, oAuth2User, schoolCampusService);
    }

    @Test
    void givenValidOAuthUser_whenDeleteCampus_thenDelegatesWithExtractedUidSchoolIdAndCampusId() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("bibbeheerder-uid");

        schoolCampusController.deleteCampus(100L, 5L, authentication);

        verify(authentication, times(2)).getPrincipal();
        verify(oAuth2User).getAttribute("userID");
        verify(schoolCampusService).deleteCampusForBibbeheerder("bibbeheerder-uid", 100L, 5L);
        verifyNoMoreInteractions(authentication, oAuth2User, schoolCampusService);
    }

    @Test
    void givenNullPrincipal_whenGetCampuses_thenThrowsUnauthorized() {
        when(authentication.getPrincipal()).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusController.getCampuses(100L, authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Niet ingelogd", exception.getReason());
        verifyNoInteractions(schoolCampusService);
    }

    @Test
    void givenMissingUid_whenGetCampuses_thenThrowsUnauthorized() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusController.getCampuses(100L, authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(schoolCampusService);
    }

    @Test
    void givenBlankUid_whenCreateCampus_thenThrowsUnauthorized() {
        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Zuid");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("   ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> schoolCampusController.createCampus(100L, request, authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
        verify(oAuth2User).getAttribute("userID");
        verifyNoInteractions(schoolCampusService);
    }
    @Test
    void givenAdminPrincipal_whenGetCampuses_thenDelegatesForPlatformAdmin() {
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setId(1L);
        AdminPrincipal adminPrincipal = new AdminPrincipal(adminEntity);

        List<SchoolCampusDTO> expected = List.of(
                new SchoolCampusDTO(1L, "Campus Noord"),
                new SchoolCampusDTO(2L, "Campus Zuid"));

        when(authentication.getPrincipal()).thenReturn(adminPrincipal);
        when(schoolCampusService.getCampusesForPlatformAdmin(100L)).thenReturn(expected);

        List<SchoolCampusDTO> result = schoolCampusController.getCampuses(100L, authentication);

        assertEquals(expected, result);
        verify(authentication).getPrincipal();
        verify(schoolCampusService).getCampusesForPlatformAdmin(100L);
        verifyNoMoreInteractions(authentication, schoolCampusService);
    }

    @Test
    void givenAdminPrincipal_whenCreateCampus_thenDelegatesForPlatformAdmin() {
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setId(1L);
        AdminPrincipal adminPrincipal = new AdminPrincipal(adminEntity);

        CreateSchoolCampusRequest request = new CreateSchoolCampusRequest("Campus Noord");
        SchoolCampusDTO expected = new SchoolCampusDTO(3L, "Campus Noord");

        when(authentication.getPrincipal()).thenReturn(adminPrincipal);
        when(schoolCampusService.createCampusForPlatformAdmin(100L, request)).thenReturn(expected);

        SchoolCampusDTO result = schoolCampusController.createCampus(100L, request, authentication);

        assertEquals(expected, result);
        verify(authentication).getPrincipal();
        verify(schoolCampusService).createCampusForPlatformAdmin(100L, request);
        verifyNoMoreInteractions(authentication, schoolCampusService);
    }

    @Test
    void givenAdminPrincipal_whenDeleteCampus_thenDelegatesForPlatformAdmin() {
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setId(1L);
        AdminPrincipal adminPrincipal = new AdminPrincipal(adminEntity);

        when(authentication.getPrincipal()).thenReturn(adminPrincipal);

        schoolCampusController.deleteCampus(100L, 5L, authentication);

        verify(authentication).getPrincipal();
        verify(schoolCampusService).deleteCampusForPlatformAdmin(100L, 5L);
        verifyNoMoreInteractions(authentication, schoolCampusService);
    }

}