package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.ReadingListDetailDTO;
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

    @Transactional(readOnly = true)
    public ReadingListDetailDTO getListDetail(Long id, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdWithBooks(id)
                .orElseThrow(() -> new IllegalArgumentException("Reading list not found"));

        // Students can only see class lists or their own personal lists.
        boolean isStaff = isStaffRole(currentUser.getRole());
        boolean isOwner = Objects.equals(list.getCreator().getId(), currentUser.getId());
        boolean isClassList = list.getListType() == ReadingListType.CLASS;

        if (!isStaff && !isOwner && !isClassList) {
            throw new AccessDeniedException("Access denied");
        }

        return toDetail(list, currentUser.getId());
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
//      Alle toegelate staff members kunnen zaken archiveren -> limiteren -> voor leerkracht -> creator check
        list.setArchived(true);
        return readingListRepository.save(list);
    }

//HELPER METHODS HIERONDER

    private UserEntity requireCurrentUser(String smartschoolUid) {
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private void requireStaff(UserEntity user) {
        if (!isStaffRole(user.getRole())) {
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    private boolean isStaffRole(UserRoles role) {
        return role == UserRoles.TEACHER
                || role == UserRoles.ADMIN
                || role == UserRoles.BIBLIOTHEEKBEHEERDER;
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

//      voorlopig smartschooluid als creatorName, later misschien nog een aparte naam veld toevoegen aan de user entity
        String creatorName = list.getCreator().getSmartschoolUid();
        return new ReadingListOverviewDTO(
                list.getId(),
                list.getTitle(),
                list.getTaskDescription(),
                list.getDeadline(),
                list.getListType(),
                list.isArchived(),
                Objects.equals(list.getCreator().getId(), currentUserId),
                creatorName,
                ids,
                ids.size()
        );
    }
    private ReadingListDetailDTO toDetail(ReadingListEntity list, Long currentUserId) {
        String creatorName = list.getCreator().getSmartschoolUid();

        List<ReadingListDetailDTO.BookItem> books = list.getBooks().stream()
                .map(book -> {
                    List<String> authors = book.getAuthors() == null
                            ? List.of()
                            : List.copyOf(book.getAuthors()); // force init while tx is open

                    return new ReadingListDetailDTO.BookItem(
                            book.getId(),
                            book.getTitle(),
                            authors,
                            book.getThumbnail(),
                            book.getIsbn()
                    );
                })
                .toList();

        return new ReadingListDetailDTO(
                list.getId(),
                list.getTitle(),
                list.getTaskDescription(),
                list.getDeadline(),
                list.getListType(),
                list.isArchived(),
                Objects.equals(list.getCreator().getId(), currentUserId),
                creatorName,
                books
        );
    }

}