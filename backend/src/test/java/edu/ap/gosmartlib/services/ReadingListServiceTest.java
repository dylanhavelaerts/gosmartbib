package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.readinglist.PublicReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListVisibilityDTO;
import edu.ap.gosmartlib.entities.BookEntities.BookEntity;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.repositories.bookRepositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.repositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.util.ReadingListType;
import edu.ap.gosmartlib.util.UserRoles;
import edu.ap.gosmartlib.util.ReadingListTargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingListServiceTest {
    @Mock
    private ReadingListRepository readingListRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private UserDirectoryService userDirectoryService;
    @InjectMocks
    private ReadingListService readingListService;

    @Test
    void givenStaffUserAndValidDto_whenCreateClassList_thenSavesClassList() {
        UserEntity teacher = user(1L, "teacher-uid", UserRoles.TEACHER);
        BookEntity book = book(10L, "Book One");
        CreateReadingListDTO dto = classYearDto("Klaslijst", "Beschrijving", "2026-05-20T12:00:00", List.of(10L));

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(bookRepository.findAllById(List.of(10L))).thenReturn(List.of(book));
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingListEntity result = readingListService.createClassList(dto, "teacher-uid");

        assertNotNull(result);
        assertEquals(ReadingListType.CLASS, result.getListType());
        assertEquals("Klaslijst", result.getTitle());
        assertEquals(LocalDateTime.parse("2026-05-20T12:00:00"), result.getDeadline());

        ArgumentCaptor<ReadingListEntity> captor = ArgumentCaptor.forClass(ReadingListEntity.class);
        verify(readingListRepository).save(captor.capture());
        ReadingListEntity captured = captor.getValue();
        assertEquals(teacher, captured.getCreator());
        assertEquals(1, captured.getBooks().size());
        assertTrue(captured.getBooks().contains(book));
        assertEquals(ReadingListTargetType.YEARS, captured.getTargetType());
        assertEquals(Set.of(5), captured.getTargetYears());
        assertFalse(captured.isTargetAllSchools());
    }

    @Test
    void givenStudentUser_whenCreateClassList_thenThrowsAccessDenied() {
        UserEntity student = user(2L, "student-uid", UserRoles.STUDENT);
        CreateReadingListDTO dto = classYearDto("Klaslijst", "Beschrijving", "2026-05-20T12:00:00", List.of(10L));
        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));

        assertThrows(AccessDeniedException.class, () -> readingListService.createClassList(dto, "student-uid"));

        verify(bookRepository, never()).findAllById(any());
        verify(readingListRepository, never()).save(any());
    }

    @Test
    void givenValidDto_whenCreatePersonalList_thenSavesPersonalListWithoutDeadline() {
        UserEntity student = user(2L, "student-uid", UserRoles.STUDENT);
        BookEntity book = book(11L, "Book Two");
        CreateReadingListDTO dto = dto("Persoonlijk", "Mijn lijst", "2027-01-01T10:15:30", List.of(11L));

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(bookRepository.findAllById(List.of(11L))).thenReturn(List.of(book));
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingListEntity result = readingListService.createPersonalList(dto, "student-uid");

        assertEquals(ReadingListType.PERSONAL, result.getListType());
        assertNull(result.getDeadline());
        assertEquals(student, result.getCreator());

        verify(bookRepository, times(1)).findAllById(List.of(11L));
        verify(readingListRepository, times(1)).save(any(ReadingListEntity.class));
    }

    @Test
    void givenOwnerPersonalList_whenUpdatePersonalList_thenUpdatesFieldsAndBooks() {
        UserEntity owner = user(10L, "owner-uid", UserRoles.STUDENT);
        ReadingListEntity existing = readingList(100L, "Old", ReadingListType.PERSONAL, owner,
                Set.of(book(1L, "Old Book")));
        BookEntity newBook = book(2L, "New Book");
        CreateReadingListDTO dto = dto("  New Title  ", "  New Description  ", "2028-01-01T00:00:00", List.of(2L));

        when(userRepository.findBySmartschoolUid("owner-uid")).thenReturn(Optional.of(owner));
        when(readingListRepository.findByIdAndCreator_Id(100L, 10L)).thenReturn(Optional.of(existing));
        when(bookRepository.findAllById(List.of(2L))).thenReturn(List.of(newBook));
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingListEntity updated = readingListService.updatePersonalList(100L, dto, "owner-uid");

        assertEquals("New Title", updated.getTitle());
        assertEquals("New Description", updated.getTaskDescription());
        assertNull(updated.getDeadline());
        assertEquals(1, updated.getBooks().size());
        assertTrue(updated.getBooks().contains(newBook));
    }

    @Test
    void givenNonPersonalList_whenUpdatePersonalList_thenThrowsAccessDenied() {
        UserEntity owner = user(10L, "owner-uid", UserRoles.STUDENT);
        ReadingListEntity classList = readingList(100L, "Class", ReadingListType.CLASS, owner, Set.of());
        CreateReadingListDTO dto = dto("Title", "Description", null, List.of());

        when(userRepository.findBySmartschoolUid("owner-uid")).thenReturn(Optional.of(owner));
        when(readingListRepository.findByIdAndCreator_Id(100L, 10L)).thenReturn(Optional.of(classList));

        assertThrows(AccessDeniedException.class, () -> readingListService.updatePersonalList(100L, dto, "owner-uid"));
        verify(readingListRepository, never()).save(any(ReadingListEntity.class));
    }

    @Test
    void givenOwnerPersonalList_whenDeletePersonalList_thenDeletes() {
        UserEntity owner = user(15L, "owner-uid", UserRoles.STUDENT);
        ReadingListEntity personalList = readingList(200L, "Personal", ReadingListType.PERSONAL, owner, Set.of());

        when(userRepository.findBySmartschoolUid("owner-uid")).thenReturn(Optional.of(owner));
        when(readingListRepository.findByIdAndCreator_Id(200L, 15L)).thenReturn(Optional.of(personalList));

        readingListService.deletePersonalList(200L, "owner-uid");

        verify(readingListRepository).delete(personalList);
    }

    @Test
    void givenClassList_whenDeletePersonalList_thenThrowsAccessDenied() {
        UserEntity owner = user(15L, "owner-uid", UserRoles.STUDENT);
        ReadingListEntity classList = readingList(200L, "Class", ReadingListType.CLASS, owner, Set.of());

        when(userRepository.findBySmartschoolUid("owner-uid")).thenReturn(Optional.of(owner));
        when(readingListRepository.findByIdAndCreator_Id(200L, 15L)).thenReturn(Optional.of(classList));

        assertThrows(AccessDeniedException.class, () -> readingListService.deletePersonalList(200L, "owner-uid"));
        verify(readingListRepository, never()).delete(any(ReadingListEntity.class));
    }

    @Test
    void givenOwnerStaffAndClassList_whenDeleteClassList_thenDeletes() {
        UserEntity teacher = user(20L, "teacher-uid", UserRoles.TEACHER);
        ReadingListEntity classList = readingList(300L, "Class", ReadingListType.CLASS, teacher, Set.of());

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(readingListRepository.findById(300L)).thenReturn(Optional.of(classList));

        readingListService.deleteClassList(300L, "teacher-uid");

        verify(readingListRepository).delete(classList);
    }

    @Test
    void givenNonOwnerStaffAndClassList_whenDeleteClassList_thenThrowsAccessDenied() {
        UserEntity teacher = user(20L, "teacher-uid", UserRoles.TEACHER);
        UserEntity otherTeacher = user(21L, "other-uid", UserRoles.TEACHER);
        ReadingListEntity classList = readingList(300L, "Class", ReadingListType.CLASS, otherTeacher, Set.of());

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(readingListRepository.findById(300L)).thenReturn(Optional.of(classList));

        assertThrows(AccessDeniedException.class, () -> readingListService.deleteClassList(300L, "teacher-uid"));
        verify(readingListRepository, never()).delete(any(ReadingListEntity.class));
    }

    @Test
    void givenPersonalList_whenDeleteClassList_thenThrowsAccessDenied() {
        UserEntity teacher = user(20L, "teacher-uid", UserRoles.TEACHER);
        ReadingListEntity personalList = readingList(301L, "Personal", ReadingListType.PERSONAL, teacher, Set.of());

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(readingListRepository.findById(301L)).thenReturn(Optional.of(personalList));

        assertThrows(AccessDeniedException.class, () -> readingListService.deleteClassList(301L, "teacher-uid"));
        verify(readingListRepository, never()).delete(any(ReadingListEntity.class));
    }

    @Test
    void givenOwnerStaffAndClassList_whenUpdateClassList_thenUpdates() {
        UserEntity teacher = user(20L, "teacher-uid", UserRoles.TEACHER);
        ReadingListEntity classList = readingList(400L, "Old Class", ReadingListType.CLASS, teacher,
                Set.of(book(5L, "Old")));
        BookEntity newBook = book(6L, "New");
        CreateReadingListDTO dto = classYearDto("  New Class  ", "  New Desc  ", "2026-10-10T12:00:00", List.of(6L));

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(readingListRepository.findById(400L)).thenReturn(Optional.of(classList));
        when(bookRepository.findAllById(List.of(6L))).thenReturn(List.of(newBook));
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingListEntity updated = readingListService.updateClassList(400L, dto, "teacher-uid");

        assertEquals("New Class", updated.getTitle());
        assertEquals("New Desc", updated.getTaskDescription());
        assertEquals(LocalDateTime.parse("2026-10-10T12:00:00"), updated.getDeadline());
        assertTrue(updated.getBooks().contains(newBook));
        assertEquals(ReadingListTargetType.YEARS, updated.getTargetType());
        assertEquals(Set.of(5), updated.getTargetYears());
        assertFalse(updated.isTargetAllSchools());
    }

    @Test
    void givenNonOwnerStaffAndClassList_whenUpdateClassList_thenThrowsAccessDenied() {
        UserEntity teacher = user(20L, "teacher-uid", UserRoles.TEACHER);
        UserEntity otherTeacher = user(99L, "other-uid", UserRoles.TEACHER);
        ReadingListEntity classList = readingList(400L, "Class", ReadingListType.CLASS, otherTeacher, Set.of());
        CreateReadingListDTO dto = dto("Title", "Desc", "2026-10-10T12:00:00", List.of());

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));
        when(readingListRepository.findById(400L)).thenReturn(Optional.of(classList));

        assertThrows(AccessDeniedException.class, () -> readingListService.updateClassList(400L, dto, "teacher-uid"));
        verify(readingListRepository, never()).save(any(ReadingListEntity.class));
    }

    @Test
    void givenDuplicateClassListsInSources_whenGetVisibleLists_thenReturnsDeduped() {
        UserEntity user = user(50L, "student-uid", UserRoles.STUDENT);
        ReadingListEntity ownPersonal = readingList(500L, "Own Personal", ReadingListType.PERSONAL, user, Set.of());
        ReadingListEntity ownClass = readingList(501L, "Own Class", ReadingListType.CLASS, user, Set.of());
        UserEntity otherTeacher = user(51L, "teacher-uid", UserRoles.TEACHER);
        ReadingListEntity otherClass = readingList(502L, "Other Class", ReadingListType.CLASS, otherTeacher, Set.of());
        otherClass.setTargetType(ReadingListTargetType.STUDENTS);
        otherClass.getTargetStudents().add(user);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(user));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(50L)).thenReturn(List.of(ownClass, ownPersonal));
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(otherClass, ownClass));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(3, result.size());
        List<Long> ids = result.stream().map(ReadingListOverviewDTO::id).toList();
        assertTrue(ids.containsAll(List.of(500L, 501L, 502L)));
    }

    @Test
    void givenStudentRequestingOtherPersonalList_whenGetListDetail_thenThrowsAccessDenied() {
        UserEntity student = user(60L, "student-uid", UserRoles.STUDENT);
        UserEntity otherStudent = user(61L, "other-student", UserRoles.STUDENT);
        ReadingListEntity personalOfOther = readingList(600L, "Other Personal", ReadingListType.PERSONAL, otherStudent,
                Set.of(book(1L, "Book")));

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findByIdWithBooks(600L)).thenReturn(Optional.of(personalOfOther));

        assertThrows(AccessDeniedException.class, () -> readingListService.getListDetail(600L, "student-uid"));
    }

    @Test
    void givenStudentRequestingClassList_whenGetListDetail_thenReturnsDetail() {
        UserEntity student = user(60L, "student-uid", UserRoles.STUDENT);
        UserEntity teacher = user(61L, "teacher-uid", UserRoles.TEACHER);
        BookEntity classBook = book(700L, "Class Book");
        classBook.setAuthors(List.of("A. Author"));
        classBook.setIsbn("isbn-700");
        ReadingListEntity classList = readingList(700L, "Class List", ReadingListType.CLASS, teacher,
                Set.of(classBook));
        classList.setTargetType(ReadingListTargetType.STUDENTS);
        classList.getTargetStudents().add(student);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findByIdWithBooks(700L)).thenReturn(Optional.of(classList));

        ReadingListDetailDTO detail = readingListService.getListDetail(700L, "student-uid");

        assertEquals(700L, detail.id());
        assertEquals("Class List", detail.title());
        assertFalse(detail.ownList());
        assertEquals(1, detail.books().size());
        assertEquals("Class Book", detail.books().get(0).title());
    }

    @Test
    void givenClassListTargetedToSpecificStudent_whenGetVisibleLists_thenStudentSeesIt() {
        UserEntity student = user(70L, "student-uid", UserRoles.STUDENT);
        UserEntity teacher = user(71L, "teacher-uid", UserRoles.TEACHER);

        ReadingListEntity classList = readingList(800L, "Targeted Student List", ReadingListType.CLASS, teacher,
                Set.of());
        classList.setTargetType(ReadingListTargetType.STUDENTS);
        classList.getTargetStudents().add(student);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(70L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(1, result.size());
        assertEquals(800L, result.get(0).id());
        assertEquals("Targeted Student List", result.get(0).title());
    }

    @Test
    void givenClassListTargetedToOtherStudent_whenGetVisibleLists_thenStudentDoesNotSeeIt() {
        UserEntity student = user(70L, "student-uid", UserRoles.STUDENT);
        UserEntity otherStudent = user(72L, "other-student-uid", UserRoles.STUDENT);
        UserEntity teacher = user(71L, "teacher-uid", UserRoles.TEACHER);

        ReadingListEntity classList = readingList(801L, "Other Student List", ReadingListType.CLASS, teacher, Set.of());
        classList.setTargetType(ReadingListTargetType.STUDENTS);
        classList.getTargetStudents().add(otherStudent);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(70L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertTrue(result.isEmpty());
    }

    @Test
    void givenClassListTargetedToClass_whenGetVisibleLists_thenStudentInThatClassSeesIt() {
        SchoolEntity school = school(1L, "Testschool");
        SchoolClassEntity targetClass = schoolClass(10L, "5ITN", school);

        UserEntity student = user(80L, "student-uid", UserRoles.STUDENT);
        student.setSchool(school);
        student.getClasses().add(targetClass);

        UserEntity teacher = user(81L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(school);

        ReadingListEntity classList = readingList(802L, "Class Target List", ReadingListType.CLASS, teacher, Set.of());
        classList.setTargetType(ReadingListTargetType.CLASSES);
        classList.getTargetClasses().add(targetClass);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(80L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(1, result.size());
        assertEquals(802L, result.get(0).id());
    }

    @Test
    void givenClassListTargetedToDifferentClass_whenGetVisibleLists_thenStudentDoesNotSeeIt() {
        SchoolEntity school = school(1L, "Testschool");
        SchoolClassEntity studentClass = schoolClass(10L, "5ITN", school);
        SchoolClassEntity otherClass = schoolClass(11L, "5LAT", school);

        UserEntity student = user(80L, "student-uid", UserRoles.STUDENT);
        student.setSchool(school);
        student.getClasses().add(studentClass);

        UserEntity teacher = user(81L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(school);

        ReadingListEntity classList = readingList(803L, "Different Class List", ReadingListType.CLASS, teacher,
                Set.of());
        classList.setTargetType(ReadingListTargetType.CLASSES);
        classList.getTargetClasses().add(otherClass);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(80L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertTrue(result.isEmpty());
    }

    @Test
    void givenYearTargetSameSchool_whenGetVisibleLists_thenStudentInThatYearSeesIt() {
        SchoolEntity school = school(1L, "Testschool");
        SchoolClassEntity studentClass = schoolClass(10L, "5ITN", school);

        UserEntity student = user(90L, "student-uid", UserRoles.STUDENT);
        student.setSchool(school);
        student.getClasses().add(studentClass);

        UserEntity teacher = user(91L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(school);

        ReadingListEntity classList = readingList(804L, "Year Target List", ReadingListType.CLASS, teacher, Set.of());
        classList.setTargetType(ReadingListTargetType.YEARS);
        classList.getTargetYears().add(5);
        classList.setTargetAllSchools(false);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(90L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(1, result.size());
        assertEquals(804L, result.get(0).id());
    }

    @Test
    void givenYearTargetDifferentSchoolAndNotAllSchools_whenGetVisibleLists_thenStudentDoesNotSeeIt() {
        SchoolEntity teacherSchool = school(1L, "Teacher School");
        SchoolEntity studentSchool = school(2L, "Student School");
        SchoolClassEntity studentClass = schoolClass(10L, "5ITN", studentSchool);

        UserEntity student = user(90L, "student-uid", UserRoles.STUDENT);
        student.setSchool(studentSchool);
        student.getClasses().add(studentClass);

        UserEntity teacher = user(91L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(teacherSchool);

        ReadingListEntity classList = readingList(805L, "Local Year Target List", ReadingListType.CLASS, teacher,
                Set.of());
        classList.setTargetType(ReadingListTargetType.YEARS);
        classList.getTargetYears().add(5);
        classList.setTargetAllSchools(false);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(90L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertTrue(result.isEmpty());
    }

    @Test
    void givenYearTargetDifferentSchoolAndAllSchools_whenGetVisibleLists_thenStudentInThatYearSeesIt() {
        SchoolEntity teacherSchool = school(1L, "Teacher School");
        SchoolEntity studentSchool = school(2L, "Student School");
        SchoolClassEntity studentClass = schoolClass(10L, "5ITN", studentSchool);

        UserEntity student = user(90L, "student-uid", UserRoles.STUDENT);
        student.setSchool(studentSchool);
        student.getClasses().add(studentClass);

        UserEntity teacher = user(91L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(teacherSchool);

        ReadingListEntity classList = readingList(806L, "Global Year Target List", ReadingListType.CLASS, teacher,
                Set.of());
        classList.setTargetType(ReadingListTargetType.YEARS);
        classList.getTargetYears().add(5);
        classList.setTargetAllSchools(true);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(90L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(1, result.size());
        assertEquals(806L, result.get(0).id());
    }

    @Test
    void givenGradeTargetAllSchools_whenGetVisibleLists_thenStudentInMatchingGradeSeesIt() {
        SchoolEntity teacherSchool = school(1L, "Teacher School");
        SchoolEntity studentSchool = school(2L, "Student School");
        SchoolClassEntity studentClass = schoolClass(10L, "6ITN", studentSchool);

        UserEntity student = user(100L, "student-uid", UserRoles.STUDENT);
        student.setSchool(studentSchool);
        student.getClasses().add(studentClass);

        UserEntity teacher = user(101L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(teacherSchool);

        ReadingListEntity classList = readingList(807L, "Grade Target List", ReadingListType.CLASS, teacher, Set.of());
        classList.setTargetType(ReadingListTargetType.GRADES);
        classList.getTargetGrades().add(3);
        classList.setTargetAllSchools(true);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(100L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(1, result.size());
        assertEquals(807L, result.get(0).id());
    }

    @Test
    void givenGradeTargetSecondGrade_whenGetVisibleLists_thenStudentInThirdGradeDoesNotSeeIt() {
        SchoolEntity school = school(1L, "Testschool");
        SchoolClassEntity studentClass = schoolClass(10L, "6ITN", school);

        UserEntity student = user(100L, "student-uid", UserRoles.STUDENT);
        student.setSchool(school);
        student.getClasses().add(studentClass);

        UserEntity teacher = user(101L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(school);

        ReadingListEntity classList = readingList(808L, "Second Grade Target List", ReadingListType.CLASS, teacher,
                Set.of());
        classList.setTargetType(ReadingListTargetType.GRADES);
        classList.getTargetGrades().add(2);
        classList.setTargetAllSchools(false);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(100L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertTrue(result.isEmpty());
    }

    @Test
    void givenClassListWithoutTargetType_whenCreateClassList_thenThrowsIllegalArgumentException() {
        UserEntity teacher = user(110L, "teacher-uid", UserRoles.TEACHER);
        CreateReadingListDTO dto = dto("Klaslijst", "Beschrijving", "2026-05-20T12:00:00", List.of());

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class, () -> readingListService.createClassList(dto, "teacher-uid"));

        verify(readingListRepository, never()).save(any());
    }

    @Test
    void givenTargetAllSchoolsForStudentTarget_whenCreateClassList_thenThrowsIllegalArgumentException() {
        UserEntity teacher = user(110L, "teacher-uid", UserRoles.TEACHER);

        CreateReadingListDTO dto = dto("Klaslijst", "Beschrijving", "2026-05-20T12:00:00", List.of());
        dto.setTargetType(ReadingListTargetType.STUDENTS);
        dto.setTargetStudentIds(List.of(1L));
        dto.setTargetAllSchools(true);

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class, () -> readingListService.createClassList(dto, "teacher-uid"));

        verify(readingListRepository, never()).save(any());
    }

    @Test
    void givenYearsAndGradesTogether_whenCreateClassListAsYearTarget_thenThrowsIllegalArgumentException() {
        UserEntity teacher = user(110L, "teacher-uid", UserRoles.TEACHER);

        CreateReadingListDTO dto = dto("Klaslijst", "Beschrijving", "2026-05-20T12:00:00", List.of());
        dto.setTargetType(ReadingListTargetType.YEARS);
        dto.setTargetYears(List.of(5));
        dto.setTargetGrades(List.of(3));

        when(userRepository.findBySmartschoolUid("teacher-uid")).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class, () -> readingListService.createClassList(dto, "teacher-uid"));

        verify(readingListRepository, never()).save(any());
    }

    @Test
    void givenPublicPersonalList_whenGetPublicListDetail_thenReturnsPublicDetail() {
        UserEntity creator = user(120L, "creator-uid", UserRoles.STUDENT);

        BookEntity book = book(900L, "Shared Book");
        book.setAuthors(List.of("A. Author"));
        book.setThumbnail("thumb.jpg");
        book.setIsbn("isbn-900");
        book.setAvailableCopies(2);

        ReadingListEntity list = readingList(
                900L,
                "Shared Personal List",
                ReadingListType.PERSONAL,
                creator,
                Set.of(book));

        list.setPublicUid("public-uid-900");
        list.setPublicVisible(true);

        when(readingListRepository.findByPublicUidWithBooks("public-uid-900"))
                .thenReturn(Optional.of(list));

        PublicReadingListDetailDTO result = readingListService.getPublicListDetail("public-uid-900");

        assertEquals("public-uid-900", result.publicUid());
        assertEquals("Shared Personal List", result.title());
        assertEquals("desc-900", result.taskDescription());
        assertEquals("STUDENT", result.creatorRole());
        assertEquals(1, result.books().size());
        assertEquals("Shared Book", result.books().get(0).title());
        assertEquals(List.of("A. Author"), result.books().get(0).authors());
        assertEquals("isbn-900", result.books().get(0).isbn());
        assertEquals(2, result.books().get(0).availableCopies());

        verify(readingListRepository, times(1)).findByPublicUidWithBooks("public-uid-900");
    }

    @Test
    void givenPrivatePersonalList_whenGetPublicListDetail_thenThrowsIllegalArgumentException() {
        UserEntity creator = user(121L, "creator-uid", UserRoles.STUDENT);

        ReadingListEntity list = readingList(
                901L,
                "Private Personal List",
                ReadingListType.PERSONAL,
                creator,
                Set.of());

        list.setPublicUid("public-uid-901");
        list.setPublicVisible(false);

        when(readingListRepository.findByPublicUidWithBooks("public-uid-901"))
                .thenReturn(Optional.of(list));

        assertThrows(
                IllegalArgumentException.class,
                () -> readingListService.getPublicListDetail("public-uid-901"));
    }

    @Test
    void givenPublicClassList_whenGetPublicListDetail_thenThrowsIllegalArgumentException() {
        UserEntity teacher = user(122L, "teacher-uid", UserRoles.TEACHER);

        ReadingListEntity list = readingList(
                902L,
                "Class List",
                ReadingListType.CLASS,
                teacher,
                Set.of());

        list.setPublicUid("public-uid-902");
        list.setPublicVisible(true);

        when(readingListRepository.findByPublicUidWithBooks("public-uid-902"))
                .thenReturn(Optional.of(list));

        assertThrows(
                IllegalArgumentException.class,
                () -> readingListService.getPublicListDetail("public-uid-902"));
    }

    @Test
    void givenUnknownPublicUid_whenGetPublicListDetail_thenThrowsIllegalArgumentException() {
        when(readingListRepository.findByPublicUidWithBooks("missing-uid"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> readingListService.getPublicListDetail("missing-uid"));
    }

    @Test
    void givenOwnerPersonalList_whenUpdatePersonalListVisibility_thenUpdatesPublicVisible() {
        UserEntity owner = user(130L, "owner-uid", UserRoles.STUDENT);

        ReadingListEntity list = readingList(
                910L,
                "Personal List",
                ReadingListType.PERSONAL,
                owner,
                Set.of());

        list.setPublicUid("public-uid-910");
        list.setPublicVisible(false);

        when(userRepository.findBySmartschoolUid("owner-uid")).thenReturn(Optional.of(owner));
        when(readingListRepository.findByIdAndCreator_Id(910L, 130L)).thenReturn(Optional.of(list));
        when(readingListRepository.save(any(ReadingListEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingListVisibilityDTO result = readingListService.updatePersonalListVisibility(
                910L,
                true,
                "owner-uid");

        assertEquals(910L, result.id());
        assertTrue(result.publicVisible());
        assertEquals("public-uid-910", result.publicUid());

        verify(readingListRepository, times(1)).save(list);
    }

    @Test
    void givenClassList_whenUpdatePersonalListVisibility_thenThrowsAccessDenied() {
        UserEntity owner = user(131L, "owner-uid", UserRoles.TEACHER);

        ReadingListEntity classList = readingList(
                911L,
                "Class List",
                ReadingListType.CLASS,
                owner,
                Set.of());

        when(userRepository.findBySmartschoolUid("owner-uid")).thenReturn(Optional.of(owner));
        when(readingListRepository.findByIdAndCreator_Id(911L, 131L)).thenReturn(Optional.of(classList));

        assertThrows(
                AccessDeniedException.class,
                () -> readingListService.updatePersonalListVisibility(911L, true, "owner-uid"));

        verify(readingListRepository, never()).save(any());
    }

    private CreateReadingListDTO dto(String title, String description, String deadline, List<Long> bookIds) {
        CreateReadingListDTO dto = new CreateReadingListDTO();
        dto.setTitle(title);
        dto.setTaskDescription(description);
        dto.setDeadline(deadline);
        dto.setBookIds(bookIds);
        return dto;
    }

    private CreateReadingListDTO classYearDto(String title, String description, String deadline, List<Long> bookIds) {
        CreateReadingListDTO dto = dto(title, description, deadline, bookIds);
        dto.setTargetType(ReadingListTargetType.YEARS);
        dto.setTargetYears(List.of(5));
        dto.setTargetAllSchools(false);
        return dto;
    }

    private UserEntity user(Long id, String uid, UserRoles role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(uid);
        user.setRole(role);
        return user;
    }

    private BookEntity book(Long id, String title) {
        BookEntity book = new BookEntity();
        book.setId(id);
        book.setTitle(title);
        book.setAuthors(List.of());
        book.setIsbn("isbn-" + id);
        return book;
    }

    private ReadingListEntity readingList(Long id, String title, ReadingListType type, UserEntity creator,
            Set<BookEntity> books) {
        ReadingListEntity list = new ReadingListEntity();
        list.setId(id);
        list.setTitle(title);
        list.setTaskDescription("desc-" + id);
        list.setListType(type);
        list.setCreator(creator);
        list.setBooks(new LinkedHashSet<>(Objects.requireNonNullElseGet(books, Set::of)));
        return list;
    }

    private SchoolEntity school(Long id, String name) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        school.setName(name);
        school.setDomain("school-" + id + ".smartschool.be");
        return school;
    }

    private SchoolClassEntity schoolClass(Long id, String name, SchoolEntity school) {
        SchoolClassEntity schoolClass = new SchoolClassEntity();
        schoolClass.setId(id);
        schoolClass.setName(name);
        schoolClass.setSchool(school);
        return schoolClass;
    }
}