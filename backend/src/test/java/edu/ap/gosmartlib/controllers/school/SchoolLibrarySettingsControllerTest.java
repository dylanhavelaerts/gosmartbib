package edu.ap.gosmartlib.controllers.school;

import edu.ap.gosmartlib.dto.school.SchoolLibrarySettingsDTO;
import edu.ap.gosmartlib.services.school.SchoolLibrarySettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolLibrarySettingsControllerTest {

    @Mock private SchoolLibrarySettingsService librarySettingsService;

    @InjectMocks
    private SchoolLibrarySettingsController schoolLibrarySettingsController;

    @Test
    void givenSchoolId_whenGetSettings_thenReturnsOkWithSettings() {
        SchoolLibrarySettingsDTO settings = mock(SchoolLibrarySettingsDTO.class);
        when(librarySettingsService.getSettings(1L)).thenReturn(settings);

        ResponseEntity<SchoolLibrarySettingsDTO> response = schoolLibrarySettingsController.getSettings(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(settings, response.getBody());
        verify(librarySettingsService).getSettings(1L);
    }

    @Test
    void givenSchoolIdAndRequest_whenSaveSettings_thenReturnsOkWithSaved() {
        SchoolLibrarySettingsDTO request = mock(SchoolLibrarySettingsDTO.class);
        SchoolLibrarySettingsDTO saved = mock(SchoolLibrarySettingsDTO.class);
        when(librarySettingsService.saveSettings(1L, request)).thenReturn(saved);

        ResponseEntity<SchoolLibrarySettingsDTO> response = schoolLibrarySettingsController.saveSettings(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(saved, response.getBody());
        verify(librarySettingsService).saveSettings(1L, request);
    }
}
