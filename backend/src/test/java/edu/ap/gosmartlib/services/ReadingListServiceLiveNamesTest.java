package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.bookRepositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.repositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.util.ReadingListTargetType;
import edu.ap.gosmartlib.util.ReadingListType;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingListServiceLiveNamesTest {

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserDirectoryService userDirectoryService;

    @InjectMocks
    private ReadingListService readingListService;

    @Test
    void givenResolvedCreatorName_whenGetVisibleLists_thenUsesLiveName() {
        SchoolEntity school = school(1L, "Testschool");
        UserEntity student = user(1L, "student-uid", UserRoles.STUDENT);
        student.setSchool(school);
        UserEntity teacher = user(2L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(school);

        ReadingListEntity classList = readingList(100L, "Klaslijst", ReadingListType.CLASS, teacher);
        classList.setTargetType(ReadingListTargetType.STUDENTS);
        classList.getTargetStudents().add(student);

        when(userRepository.findDetailedBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(1L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        when(userDirectoryService.resolveDisplayNames(any(), any()))
                .thenReturn(new ResolveDisplayNamesResponse(
                        true,
                        1,
                        1,
                        Map.of("teacher-uid", "Creator Name"),
                        List.of(),
                        "ok"));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(1, result.size());
        assertEquals("Creator Name", result.get(0).creatorName());
    }

    @Test
    void givenUnresolvedCreatorName_whenGetVisibleLists_thenFallsBackToRoleLabel() {
        SchoolEntity school = school(1L, "Testschool");

        UserEntity student = user(1L, "student-uid", UserRoles.STUDENT);
        student.setSchool(school);

        UserEntity teacher = user(2L, "teacher-uid", UserRoles.TEACHER);
        teacher.setSchool(school);

        ReadingListEntity classList = readingList(100L, "Klaslijst", ReadingListType.CLASS, teacher);
        classList.setTargetType(ReadingListTargetType.STUDENTS);
        classList.getTargetStudents().add(student);

        when(userRepository.findDetailedBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(1L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        List<ReadingListOverviewDTO> result = readingListService.getVisibleLists("student-uid");

        assertEquals(1, result.size());
        assertEquals("Leerkracht", result.get(0).creatorName());
    }

    private UserEntity user(Long id, String uid, UserRoles role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(uid);
        user.setRole(role);
        return user;
    }

    private ReadingListEntity readingList(Long id, String title, ReadingListType type, UserEntity creator) {
        ReadingListEntity list = new ReadingListEntity();
        list.setId(id);
        list.setPublicUid("public-uid-" + id);
        list.setPublicVisible(false);
        list.setTitle(title);
        list.setTaskDescription("desc");
        list.setListType(type);
        list.setCreator(creator);
        list.setBooks(Set.of());
        return list;
    }

    private SchoolEntity school(Long id, String name) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        school.setName(name);
        school.setDomain("school-" + id + ".smartschool.be");
        return school;
    }
}