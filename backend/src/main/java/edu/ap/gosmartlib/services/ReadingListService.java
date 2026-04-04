package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.ReadingListOverviewDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.ReadingListType;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReadingListService {

    private final ReadingListRepository readingListRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;


    @Transactional(readOnly = true)
        public List<ReadingListOverviewDTO> getVisibleLists(String smartschoolUid) {
            UserEntity currentUser = requireCurrentUser(smartschoolUid);

            // Use a LinkedHashMap keyed on id to prevent duplicates when a staff member
            // created a class list (it would otherwise appear in both queries).
            Map<Long, ReadingListEntity> deduped = new LinkedHashMap<>();

            readingListRepository.findAllByCreator_IdOrderByIdDesc(currentUser.getId())
                    .forEach(list -> deduped.put(list.getId(), list));

            readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS)
                    .forEach(list -> deduped.putIfAbsent(list.getId(), list));

            return deduped.values().stream()
                    .filter(list -> !list.isArchived() || list.getListType() == ReadingListType.PERSONAL)
                    .map(list -> toOverview(list, currentUser.getId()))
                    .toList();
    }

    @Transactional
    public ReadingListEntity createClassList(CreateReadingListDTO dto, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);
        requireStaff(currentUser);

        validateTitle(dto.getTitle());

        ReadingListEntity list = new ReadingListEntity();
        list.setTitle(dto.getTitle().trim());
        list.setTaskDescription(normalizeText(dto.getTaskDescription()));
        list.setDeadline(parseOptionalDeadline(dto.getDeadline()));
        list.setCreator(currentUser);
        list.setListType(ReadingListType.CLASS);
        list.setArchived(false);
        list.getBooks().addAll(loadBooks(dto.getBookIds()));

        return readingListRepository.save(list);
    }

    @Transactional
    public ReadingListEntity createPersonalList(CreateReadingListDTO dto, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        validateTitle(dto.getTitle());

        ReadingListEntity list = new ReadingListEntity();
        list.setTitle(dto.getTitle().trim());
        list.setTaskDescription(normalizeText(dto.getTaskDescription()));
        list.setDeadline(null); // personal lists have no deadline
        list.setCreator(currentUser);
        list.setListType(ReadingListType.PERSONAL);
        list.setArchived(false);
        list.getBooks().addAll(loadBooks(dto.getBookIds()));

        return readingListRepository.save(list);
    }

    @Transactional
    public ReadingListEntity updatePersonalList(Long id, CreateReadingListDTO dto, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdAndCreator_Id(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personal reading list not found"));

        if (list.getListType() != ReadingListType.PERSONAL) {
            throw new AccessDeniedException("Only personal lists can be updated here");
        }

        validateTitle(dto.getTitle());

        list.setTitle(dto.getTitle().trim());
        list.setTaskDescription(normalizeText(dto.getTaskDescription()));
        list.setDeadline(null); // keep personal lists without deadline
        list.getBooks().clear();
        list.getBooks().addAll(loadBooks(dto.getBookIds()));

        return readingListRepository.save(list);
    }

    @Transactional
    public void deletePersonalList(Long id, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdAndCreator_Id(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personal reading list not found"));

        if (list.getListType() != ReadingListType.PERSONAL) {
            throw new AccessDeniedException("Only personal lists can be deleted here");
        }

        readingListRepository.delete(list);
    }

    @Transactional
    public ReadingListEntity archiveClassList(Long id, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);
        requireStaff(currentUser);

        ReadingListEntity list = readingListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Class reading list not found"));

        if (list.getListType() != ReadingListType.CLASS) {
            throw new AccessDeniedException("Only class lists can be archived");
        }

        list.setArchived(true);
        return readingListRepository.save(list);
    }

    private UserEntity requireCurrentUser(String smartschoolUid) {
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private void requireStaff(UserEntity user) {
        UserRoles role = user.getRole();
        boolean allowed = role == UserRoles.TEACHER
                || role == UserRoles.ADMIN
                || role == UserRoles.BIBLIOTHEEKBEHEERDER;

        if (!allowed) {
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
    }

    private String normalizeText(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private LocalDateTime parseOptionalDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(deadline);
    }

    private List<BookEntity> loadBooks(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }

        List<Long> sanitizedIds = bookIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return bookRepository.findAllById(sanitizedIds);
    }

    private ReadingListOverviewDTO toOverview(ReadingListEntity list, Long currentUserId) {
        List<Long> ids = list.getBooks().stream().map(BookEntity::getId).toList();

        return ReadingListOverviewDTO.builder()
                .id(list.getId())
                .title(list.getTitle())
                .taskDescription(list.getTaskDescription())
                .deadline(list.getDeadline())
                .listType(list.getListType())
                .archived(list.isArchived())
                .ownList(Objects.equals(list.getCreator().getId(), currentUserId))
                .creatorName("User " + list.getCreator().getId())
                .bookIds(ids)
                .bookCount(ids.size())
                .build();
    }
}