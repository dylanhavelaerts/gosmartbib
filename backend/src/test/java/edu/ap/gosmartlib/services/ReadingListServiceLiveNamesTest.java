package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
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
    private BookRepository bookRepository;

    @Mock
    private UserDirectoryService userDirectoryService;

    @InjectMocks
    private ReadingListService readingListService;

    @Test
    void givenResolvedCreatorName_whenGetVisibleLists_thenUsesLiveName() {
        UserEntity student = user(1L, "student-uid", UserRoles.STUDENT);
        UserEntity teacher = user(2L, "teacher-uid", UserRoles.TEACHER);

        ReadingListEntity classList = readingList(100L, "Klaslijst", ReadingListType.CLASS, teacher);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
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
        UserEntity student = user(1L, "student-uid", UserRoles.STUDENT);
        UserEntity teacher = user(2L, "teacher-uid", UserRoles.TEACHER);

        ReadingListEntity classList = readingList(100L, "Klaslijst", ReadingListType.CLASS, teacher);

        when(userRepository.findBySmartschoolUid("student-uid")).thenReturn(Optional.of(student));
        when(readingListRepository.findAllByCreator_IdOrderByIdDesc(1L)).thenReturn(List.of());
        when(readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS))
                .thenReturn(List.of(classList));

        when(userDirectoryService.resolveDisplayNames(any(), any()))
                .thenReturn(new ResolveDisplayNamesResponse(
                        true,
                        1,
                        0,
                        Map.of(),
                        List.of("teacher-uid"),
                        "not found"));

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
        list.setTitle(title);
        list.setTaskDescription("desc");
        list.setListType(type);
        list.setCreator(creator);
        list.setBooks(Set.of());
        return list;
    }
}