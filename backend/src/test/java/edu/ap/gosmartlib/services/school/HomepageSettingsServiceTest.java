package edu.ap.gosmartlib.services.school;

import edu.ap.gosmartlib.dto.school.HomepageSettingsDTO;
import edu.ap.gosmartlib.entities.HomepageSettingsEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.repositories.HomepageSettingsRepository;
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
class HomepageSettingsServiceTest {

    @Mock private HomepageSettingsRepository homepageSettingsRepository;
    @Mock private SchoolRepository schoolRepository;

    @InjectMocks private HomepageSettingsService homepageSettingsService;

    private static final Long SCHOOL_ID = 1L;

    private SchoolEntity school;
    private HomepageSettingsEntity settingsEntity;

    @BeforeEach
    void setUp() {
        school = new SchoolEntity();
        school.setId(SCHOOL_ID);

        settingsEntity = new HomepageSettingsEntity(school);
        settingsEntity.setShowSpotlight(true);
        settingsEntity.setShowNewInLibrary(false);
        settingsEntity.setShowReadingLists(true);
        settingsEntity.setShowUrgentLoans(false);
        settingsEntity.setSmartschoolSenderIdentifier("sender@school.be");
    }

    @Test
    void givenExistingSettings_whenGetSettings_thenReturnsMappedDTO() {
        when(homepageSettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(settingsEntity));

        HomepageSettingsDTO result = homepageSettingsService.getSettings(SCHOOL_ID);

        assertEquals(SCHOOL_ID, result.schoolId());
        assertTrue(result.showSpotlight());
        assertFalse(result.showNewInLibrary());
        assertTrue(result.showReadingLists());
        assertFalse(result.showUrgentLoans());
        assertEquals("sender@school.be", result.smartschoolSenderIdentifier());
    }

    @Test
    void givenNoExistingSettings_whenGetSettings_thenReturnsDefaultDTO() {
        when(homepageSettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        HomepageSettingsDTO result = homepageSettingsService.getSettings(SCHOOL_ID);

        assertEquals(SCHOOL_ID, result.schoolId());
        assertTrue(result.showSpotlight());
        assertTrue(result.showNewInLibrary());
        assertTrue(result.showReadingLists());
        assertTrue(result.showUrgentLoans());
        assertNull(result.smartschoolSenderIdentifier());
    }

    @Test
    void givenExistingSettings_whenSaveSettings_thenUpdatesAndReturnsDTO() {
        HomepageSettingsDTO request = new HomepageSettingsDTO(SCHOOL_ID, false, true, false, true, "  new@school.be  ");
        when(homepageSettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(settingsEntity));
        when(homepageSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HomepageSettingsDTO result = homepageSettingsService.saveSettings(SCHOOL_ID, request);

        assertFalse(result.showSpotlight());
        assertTrue(result.showNewInLibrary());
        assertFalse(result.showReadingLists());
        assertTrue(result.showUrgentLoans());
        assertEquals("new@school.be", result.smartschoolSenderIdentifier());
        verify(schoolRepository, never()).findById(any());
    }

    @Test
    void givenNoExistingSettings_whenSaveSettings_thenCreatesNewSettingsForSchool() {
        HomepageSettingsDTO request = new HomepageSettingsDTO(SCHOOL_ID, true, true, true, true, null);
        when(homepageSettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(homepageSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HomepageSettingsDTO result = homepageSettingsService.saveSettings(SCHOOL_ID, request);

        assertTrue(result.showSpotlight());
        assertNull(result.smartschoolSenderIdentifier());

        ArgumentCaptor<HomepageSettingsEntity> captor = ArgumentCaptor.forClass(HomepageSettingsEntity.class);
        verify(homepageSettingsRepository).save(captor.capture());
        assertEquals(school, captor.getValue().getSchool());
    }

    @Test
    void givenSchoolNotFound_whenSaveSettings_thenThrows404() {
        HomepageSettingsDTO request = new HomepageSettingsDTO(SCHOOL_ID, true, true, true, true, null);
        when(homepageSettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> homepageSettingsService.saveSettings(SCHOOL_ID, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenSenderIdentifierWithWhitespace_whenSaveSettings_thenTrimsSenderIdentifier() {
        HomepageSettingsDTO request = new HomepageSettingsDTO(SCHOOL_ID, true, true, true, true, "  trimmed@school.be  ");
        when(homepageSettingsRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(settingsEntity));
        when(homepageSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HomepageSettingsDTO result = homepageSettingsService.saveSettings(SCHOOL_ID, request);

        assertEquals("trimmed@school.be", result.smartschoolSenderIdentifier());
    }
}
