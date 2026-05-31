package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.repositories.school.SchoolClassRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolClassHelperTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @InjectMocks
    private SchoolClassHelper schoolClassHelper;

    @Test
    void givenExistingClassByGroupId_whenFindOrCreate_thenReturnsExistingClass() {
        SchoolEntity school = school(1L);
        SchoolClassEntity existing = schoolClass(10L, school, "group-1", "2ITSOF");

        when(schoolClassRepository.findBySmartschoolGroupId("group-1"))
                .thenReturn(Optional.of(existing));

        SchoolClassEntity result = schoolClassHelper.findOrCreate(
                school,
                "group-1",
                "2ITSOF",
                "2025-2026",
                "1");

        assertSame(existing, result);
        verify(schoolClassRepository, never()).findFirstBySchool_IdAndNameIgnoreCase(anyLong(), anyString());
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void givenExistingClassByName_whenFindOrCreate_thenReusesClassAndUpdatesExternalData() {
        SchoolEntity school = school(1L);
        SchoolClassEntity existing = schoolClass(10L, school, "old-group", "2ITSOF");

        when(schoolClassRepository.findBySmartschoolGroupId("new-group"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findFirstBySchool_IdAndNameIgnoreCase(1L, "2ITSOF"))
                .thenReturn(Optional.of(existing));
        when(schoolClassRepository.save(existing)).thenReturn(existing);

        SchoolClassEntity result = schoolClassHelper.findOrCreate(
                school,
                "new-group",
                "2ITSOF",
                "2025-2026",
                "1");

        assertSame(existing, result);
        assertEquals("new-group", existing.getSmartschoolGroupId());
        assertEquals("2025-2026", existing.getSchoolYear());
        assertEquals("1", existing.getGrade());
        verify(schoolClassRepository).save(existing);
    }

    @Test
    void givenNoExistingClass_whenFindOrCreate_thenCreatesNewClass() {
        SchoolEntity school = school(1L);

        when(schoolClassRepository.findBySmartschoolGroupId("group-1"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findFirstBySchool_IdAndNameIgnoreCase(1L, "2ITSOF"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.save(any(SchoolClassEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SchoolClassEntity result = schoolClassHelper.findOrCreate(
                school,
                "group-1",
                "2ITSOF",
                "2025-2026",
                "1");

        assertSame(school, result.getSchool());
        assertEquals("group-1", result.getSmartschoolGroupId());
        assertEquals("2ITSOF", result.getName());
        assertEquals("2025-2026", result.getSchoolYear());
        assertEquals("1", result.getGrade());
    }

    @Test
    void givenUniqueConstraintViolationByClassName_whenFindOrCreate_thenReturnsExistingClassByName() {
        SchoolEntity school = school(1L);
        SchoolClassEntity existing = schoolClass(10L, school, "existing-group", "2ITSOF");

        when(schoolClassRepository.findBySmartschoolGroupId("new-group"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findFirstBySchool_IdAndNameIgnoreCase(1L, "2ITSOF"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(schoolClassRepository.save(any(SchoolClassEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate school class"));

        SchoolClassEntity result = schoolClassHelper.findOrCreate(
                school,
                "new-group",
                "2ITSOF",
                "2025-2026",
                "1");

        assertSame(existing, result);
    }

    private SchoolEntity school(Long id) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        school.setName("School " + id);
        return school;
    }

    private SchoolClassEntity schoolClass(Long id, SchoolEntity school, String groupId, String name) {
        SchoolClassEntity schoolClass = new SchoolClassEntity();
        schoolClass.setId(id);
        schoolClass.setSchool(school);
        schoolClass.setSmartschoolGroupId(groupId);
        schoolClass.setName(name);
        return schoolClass;
    }
}