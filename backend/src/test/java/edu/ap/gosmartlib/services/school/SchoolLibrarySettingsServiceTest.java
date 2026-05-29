package edu.ap.gosmartlib.services.school;

import edu.ap.gosmartlib.dto.school.SchoolLibrarySettingsDTO;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.school.SchoolLibrarySettingsEntity;
import edu.ap.gosmartlib.repositories.school.SchoolLibrarySettingsRepository;
import edu.ap.gosmartlib.repositories.school.SchoolRepository;
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
class SchoolLibrarySettingsServiceTest {

    @Mock private SchoolLibrarySettingsRepository librarySettingsRepository;
    @Mock private SchoolRepository schoolRepository;

    @InjectMocks private SchoolLibrarySettingsService schoolLibrarySettingsService;

    private static final Long SCHOOL_ID = 1L;

    private SchoolEntity school;
    private SchoolLibrarySettingsEntity settingsEntity;

    @BeforeEach
    void setUp() {
        school = new SchoolEntity();
        school.setId(SCHOOL_ID);

        settingsEntity = new SchoolLibrarySettingsEntity(school);
        settingsEntity.setBarcodesEnabled(true);
    }

    @Test
    void givenExistingSettings_whenGetSettings_thenReturnsMappedDTO() {
        when(librarySettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(settingsEntity));

        SchoolLibrarySettingsDTO result = schoolLibrarySettingsService.getSettings(SCHOOL_ID);

        assertEquals(SCHOOL_ID, result.schoolId());
        assertTrue(result.barcodesEnabled());
    }

    @Test
    void givenNoExistingSettings_whenGetSettings_thenReturnsDefaultDTOWithBarcodesEnabled() {
        when(librarySettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        SchoolLibrarySettingsDTO result = schoolLibrarySettingsService.getSettings(SCHOOL_ID);

        assertEquals(SCHOOL_ID, result.schoolId());
        assertTrue(result.barcodesEnabled());
    }

    @Test
    void givenExistingSettings_whenSaveSettings_thenUpdatesBarcodesEnabledAndReturnsDTO() {
        SchoolLibrarySettingsDTO request = new SchoolLibrarySettingsDTO(SCHOOL_ID, false);
        when(librarySettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(settingsEntity));
        when(librarySettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolLibrarySettingsDTO result = schoolLibrarySettingsService.saveSettings(SCHOOL_ID, request);

        assertFalse(result.barcodesEnabled());
        verify(schoolRepository, never()).findById(any());
    }

    @Test
    void givenNoExistingSettings_whenSaveSettings_thenCreatesNewSettingsForSchool() {
        SchoolLibrarySettingsDTO request = new SchoolLibrarySettingsDTO(SCHOOL_ID, false);
        when(librarySettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(librarySettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolLibrarySettingsDTO result = schoolLibrarySettingsService.saveSettings(SCHOOL_ID, request);

        assertFalse(result.barcodesEnabled());

        ArgumentCaptor<SchoolLibrarySettingsEntity> captor = ArgumentCaptor.forClass(SchoolLibrarySettingsEntity.class);
        verify(librarySettingsRepository).save(captor.capture());
        assertEquals(school, captor.getValue().getSchool());
    }

    @Test
    void givenSchoolNotFound_whenSaveSettings_thenThrows404() {
        SchoolLibrarySettingsDTO request = new SchoolLibrarySettingsDTO(SCHOOL_ID, true);
        when(librarySettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> schoolLibrarySettingsService.saveSettings(SCHOOL_ID, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenBarcodesCurrentlyEnabled_whenSaveSettingsWithDisabled_thenTogglesOff() {
        settingsEntity.setBarcodesEnabled(true);
        SchoolLibrarySettingsDTO request = new SchoolLibrarySettingsDTO(SCHOOL_ID, false);
        when(librarySettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(settingsEntity));
        when(librarySettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolLibrarySettingsDTO result = schoolLibrarySettingsService.saveSettings(SCHOOL_ID, request);

        assertFalse(result.barcodesEnabled());
    }
}
