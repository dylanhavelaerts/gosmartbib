package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
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
    private final UserDirectoryService userDirectoryService;

    @Transactional(readOnly = true)
    public List<ReadingListOverviewDTO> getVisibleLists(String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        Map<Long, ReadingListEntity> deduped = new LinkedHashMap<>();

        readingListRepository.findAllByCreator_IdOrderByIdDesc(currentUser.getId())
                .forEach(list -> deduped.put(list.getId(), list));

        readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS)
                .forEach(list -> deduped.putIfAbsent(list.getId(), list));

        List<String> creatorUids = deduped.values().stream()
                .map(list -> list.getCreator().getSmartschoolUid())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(uid -> !uid.isBlank())
                .distinct()
                .toList();

        Map<String, String> displayNames = resolveDisplayNamesMap(smartschoolUid, creatorUids);

        return deduped.values().stream()
                .map(list -> toOverview(list, currentUser.getId(), displayNames))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReadingListDetailDTO getListDetail(Long id, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdWithBooks(id)
                .orElseThrow(() -> new IllegalArgumentException("LeesLijst niet gevonden"));

        // Leerlingen kunnen enkel hun eigen lijst of klaslijsten zien.
        boolean isStaff = isStaffRole(currentUser.getRole());
        boolean isOwner = Objects.equals(list.getCreator().getId(), currentUser.getId());
        boolean isClassList = list.getListType() == ReadingListType.CLASS;

        if (!isStaff && !isOwner && !isClassList) {
            throw new AccessDeniedException("Toegang geweigerd: deze leeslijst is niet zichtbaar voor jou");
        }

        String creatorUid = list.getCreator().getSmartschoolUid();
        Map<String, String> displayNames = resolveDisplayNamesMap(
                smartschoolUid,
                creatorUid == null ? List.of() : List.of(creatorUid));

        return toDetail(list, currentUser.getId(), displayNames);
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
        list.setDeadline(null);
        list.setCreator(currentUser);
        list.setListType(ReadingListType.PERSONAL);
        list.getBooks().addAll(loadBooks(dto.getBookIds()));

        return readingListRepository.save(list);
    }

    @Transactional
    public ReadingListEntity updatePersonalList(Long id, CreateReadingListDTO dto, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdAndCreator_Id(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Persoonlijke leeslijst niet gevonden"));

        if (list.getListType() != ReadingListType.PERSONAL) {
            throw new AccessDeniedException("Alleen persoonlijke lijsten kunnen hier worden aangepast");
        }

        validateTitle(dto.getTitle());

        list.setTitle(dto.getTitle().trim());
        list.setTaskDescription(normalizeText(dto.getTaskDescription()));
        list.setDeadline(null);
        list.getBooks().clear();
        list.getBooks().addAll(loadBooks(dto.getBookIds()));

        return readingListRepository.save(list);
    }

    @Transactional
    public void deletePersonalList(Long id, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdAndCreator_Id(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Persoonlijke lijst niet gevonden"));

        if (list.getListType() != ReadingListType.PERSONAL) {
            throw new AccessDeniedException("Alleen persoonlijke lijsten kunnen hier worden verwijderd");
        }

        readingListRepository.delete(list);
    }

    @Transactional
    public void deleteClassList(Long id, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);
        requireStaff(currentUser);

        ReadingListEntity list = readingListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Klasleeslijst niet gevonden"));

        if (list.getListType() != ReadingListType.CLASS) {
            throw new AccessDeniedException("Alleen klaslijsten kunnen hier worden verwijderd");
        }

        if (!Objects.equals(list.getCreator().getId(), currentUser.getId())) {
            throw new AccessDeniedException("Je kan alleen klaslijsten verwijderen die je zelf hebt aangemaakt");
        }

        readingListRepository.delete(list);
    }

    @Transactional
    public ReadingListEntity updateClassList(Long id, CreateReadingListDTO dto, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);
        requireStaff(currentUser);
        ReadingListEntity list = readingListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Klasleeslijst niet gevonden"));
        if (list.getListType() != ReadingListType.CLASS) {
            throw new AccessDeniedException("Alleen klaslijsten kunnen hier worden aangepast");
        }
        if (!Objects.equals(list.getCreator().getId(), currentUser.getId())) {
            throw new AccessDeniedException("Je kan alleen klaslijsten aanpassen die je zelf hebt aangemaakt");
        }
        validateTitle(dto.getTitle());
        list.setTitle(dto.getTitle().trim());
        list.setTaskDescription(normalizeText(dto.getTaskDescription()));
        list.setDeadline(parseOptionalDeadline(dto.getDeadline()));
        list.getBooks().clear();
        list.getBooks().addAll(loadBooks(dto.getBookIds()));
        return readingListRepository.save(list);
    }

// region Helper methods

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

    private Map<String, String> resolveDisplayNamesMap(String actorUid, List<String> uids) {
        if (uids == null || uids.isEmpty()) {
            return Map.of();
        }

        try {
            var response = userDirectoryService.resolveDisplayNames(
                    actorUid,
                    new ResolveDisplayNamesRequest(uids));

            if (!response.success() || response.displayNames() == null) {
                return Map.of();
            }

            return response.displayNames();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String resolveCreatorName(ReadingListEntity list, Map<String, String> displayNames) {
        String creatorUid = list.getCreator().getSmartschoolUid();
        if (creatorUid == null || creatorUid.isBlank()) {
            return formatRoleLabel(list.getCreator().getRole());
        }

        return displayNames.getOrDefault(
                creatorUid,
                formatRoleLabel(list.getCreator().getRole()));
    }

    private ReadingListOverviewDTO toOverview(ReadingListEntity list, Long currentUserId,
                                              Map<String, String> displayNames) {
        List<Long> ids = list.getBooks().stream().map(BookEntity::getId).toList();

        String creatorName = resolveCreatorName(list, displayNames);
        return new ReadingListOverviewDTO(
                list.getId(),
                list.getTitle(),
                list.getTaskDescription(),
                list.getDeadline(),
                list.getListType(),
                Objects.equals(list.getCreator().getId(), currentUserId),
                creatorName,
                ids,
                ids.size());
    }

    private String formatRoleLabel(UserRoles role) {
        if (role == null) {
            return "Gebruiker";
        }

        return switch (role) {
            case STUDENT -> "Leerling";
            case TEACHER -> "Leerkracht";
            case BIBLIOTHEEKBEHEERDER -> "Bibliothecaris";
            case ADMIN -> "Admin";
            case OTHER -> "Gebruiker";
        };
    }

    private ReadingListDetailDTO toDetail(ReadingListEntity list, Long currentUserId,
                                          Map<String, String> displayNames) {
        String creatorName = resolveCreatorName(list, displayNames);

        List<ReadingListDetailDTO.BookItem> books = list.getBooks().stream()
                .map(book -> {
                    List<String> authors = book.getAuthors() == null
                            ? List.of()
                            : List.copyOf(book.getAuthors());

                    return new ReadingListDetailDTO.BookItem(
                            book.getId(),
                            book.getTitle(),
                            authors,
                            book.getThumbnail(),
                            book.getIsbn(),
                            book.getAvailableCopies() != null ? book.getAvailableCopies() : 0);

                })
                .toList();

        return new ReadingListDetailDTO(
                list.getId(),
                list.getTitle(),
                list.getTaskDescription(),
                list.getDeadline(),
                list.getListType(),
                Objects.equals(list.getCreator().getId(), currentUserId),
                creatorName,
                books);
    }
    // endregion

}