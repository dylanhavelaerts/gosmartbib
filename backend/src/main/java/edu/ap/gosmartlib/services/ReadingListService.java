package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.PublicReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListBookDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListVisibilityDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.repositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.util.ReadingListTargetType;
import edu.ap.gosmartlib.util.ReadingListType;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingListService {

    private static final List<Integer> VALID_YEARS = List.of(1, 2, 3, 4, 5, 6, 7);
    private static final List<Integer> VALID_GRADES = List.of(1, 2, 3);
    private final ReadingListRepository readingListRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserDirectoryService userDirectoryService;

    @Transactional(readOnly = true)
    public List<ReadingListOverviewDTO> getVisibleLists(String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        Map<Long, ReadingListEntity> deduped = new LinkedHashMap<>();

        readingListRepository.findAllByCreator_IdOrderByIdDesc(currentUser.getId())
                .forEach(list -> deduped.put(list.getId(), list));

        readingListRepository.findAllByListTypeOrderByIdDesc(ReadingListType.CLASS).stream()
                .filter(list -> canViewClassList(list, currentUser))
                .forEach(list -> deduped.putIfAbsent(list.getId(), list));

        List<String> userUidsToResolve = collectRelevantUserUids(deduped.values(), currentUser);
        Map<String, String> displayNames = resolveDisplayNamesMap(smartschoolUid, userUidsToResolve);

        return deduped.values().stream()
                .map(list -> toOverview(list, currentUser, displayNames))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReadingListDetailDTO getListDetail(Long id, String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdWithBooks(id)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst niet gevonden"));

        boolean isOwner = Objects.equals(list.getCreator().getId(), currentUser.getId());
        boolean isClassList = list.getListType() == ReadingListType.CLASS;

        if (isClassList && !canViewClassList(list, currentUser)) {
            throw new AccessDeniedException("Toegang geweigerd: deze klasleeslijst is niet zichtbaar voor jou");
        }

        if (!isClassList && !isOwner) {
            throw new AccessDeniedException(
                    "Toegang geweigerd: deze persoonlijke leeslijst is alleen zichtbaar voor de eigenaar");
        }

        List<String> userUidsToResolve = collectRelevantUserUids(List.of(list), currentUser);
        Map<String, String> displayNames = resolveDisplayNamesMap(smartschoolUid, userUidsToResolve);

        return toDetail(list, currentUser, displayNames);
    }

    @Transactional(readOnly = true)
    public PublicReadingListDetailDTO getPublicListDetail(String publicUid) {
        if (publicUid == null || publicUid.isBlank()) {
            throw new IllegalArgumentException("Publieke leeslijst niet gevonden");
        }

        ReadingListEntity list = readingListRepository.findByPublicUidWithBooks(publicUid.trim())
                .orElseThrow(() -> new IllegalArgumentException("Publieke leeslijst niet gevonden"));

        if (list.getListType() != ReadingListType.PERSONAL || !list.isPublicVisible()) {
            throw new IllegalArgumentException("Publieke leeslijst niet gevonden");
        }

        return toPublicDetailDTO(list);
    }

    @Transactional
    public ReadingListVisibilityDTO updatePersonalListVisibility(Long id, boolean publicVisible,
            String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);

        ReadingListEntity list = readingListRepository.findByIdAndCreator_Id(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Persoonlijke leeslijst niet gevonden"));

        if (list.getListType() != ReadingListType.PERSONAL) {
            throw new AccessDeniedException("Alleen persoonlijke leeslijsten kunnen publiek gedeeld worden");
        }

        if (list.getPublicUid() == null || list.getPublicUid().isBlank()) {
            list.setPublicUid(UUID.randomUUID().toString());
        }

        list.setPublicVisible(publicVisible);

        ReadingListEntity saved = readingListRepository.save(list);

        return new ReadingListVisibilityDTO(
                saved.getId(),
                saved.getPublicUid(),
                saved.isPublicVisible());
    }

    @Transactional(readOnly = true)
    public ReadingListAssignmentTargetsDTO getAssignmentTargets(String smartschoolUid) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);
        requireStaff(currentUser);

        Long schoolId = requireSchoolId(currentUser);

        List<ReadingListAssignmentTargetsDTO.ClassTarget> classes = schoolClassRepository
                .findAllBySchool_IdOrderByNameAsc(schoolId)
                .stream()
                .map(schoolClass -> {
                    Integer year = resolveYearFromClassName(schoolClass.getName());

                    return new ReadingListAssignmentTargetsDTO.ClassTarget(
                            schoolClass.getId(),
                            schoolClass.getName(),
                            year,
                            resolveGradeFromYear(year));
                })
                .toList();

        return new ReadingListAssignmentTargetsDTO(List.of(), classes, VALID_YEARS, VALID_GRADES);
    }

    @Transactional(readOnly = true)
    public List<ReadingListAssignmentTargetsDTO.StudentTarget> searchAssignmentStudents(
            String smartschoolUid,
            String query) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);
        requireStaff(currentUser);

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        Long schoolId = requireSchoolId(currentUser);

        List<UserEntity> studentsInSchool = userRepository
                .findAllBySchool_IdOrderBySmartschoolUidAsc(schoolId)
                .stream()
                .filter(user -> user.getRole() == UserRoles.STUDENT)
                .toList();

        if (studentsInSchool.isEmpty()) {
            return List.of();
        }

        List<String> studentUids = studentsInSchool.stream()
                .map(UserEntity::getSmartschoolUid)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(uid -> !uid.isBlank())
                .distinct()
                .toList();

        Map<String, String> displayNames = resolveDisplayNamesMap(smartschoolUid, studentUids);

        List<ReadingListAssignmentTargetsDTO.StudentTarget> matches = new ArrayList<>();

        for (UserEntity student : studentsInSchool) {
            String displayName = resolveStudentDisplayName(student, displayNames);
            List<String> classNames = sortedClassNames(student.getClasses());

            if (!matchesStudentSearch(student, displayName, classNames, normalizedQuery)) {
                continue;
            }

            matches.add(new ReadingListAssignmentTargetsDTO.StudentTarget(
                    student.getId(),
                    displayName,
                    classNames));
        }

        return matches.stream()
                .sorted(Comparator.comparing(
                        ReadingListAssignmentTargetsDTO.StudentTarget::displayName,
                        String.CASE_INSENSITIVE_ORDER))
                .limit(20)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingListAssignmentTargetsDTO.ClassTarget> searchAssignmentClasses(
            String smartschoolUid,
            String query) {
        UserEntity currentUser = requireCurrentUser(smartschoolUid);
        requireStaff(currentUser);

        String normalizedQuery = query == null ? "" : query.trim();
        Long schoolId = requireSchoolId(currentUser);

        if (normalizedQuery.isEmpty()) {
            return schoolClassRepository
                    .findAllBySchool_IdOrderByNameAsc(schoolId)
                    .stream()
                    .map(schoolClass -> {
                        Integer year = resolveYearFromClassName(schoolClass.getName());
                        return new ReadingListAssignmentTargetsDTO.ClassTarget(
                                schoolClass.getId(),
                                schoolClass.getName(),
                                year,
                                resolveGradeFromYear(year));
                    })
                    .toList();
        }

        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        return schoolClassRepository
                .findTop20BySchool_IdAndNameContainingIgnoreCaseOrderByNameAsc(schoolId, normalizedQuery)
                .stream()
                .map(schoolClass -> {
                    Integer year = resolveYearFromClassName(schoolClass.getName());
                    return new ReadingListAssignmentTargetsDTO.ClassTarget(
                            schoolClass.getId(),
                            schoolClass.getName(),
                            year,
                            resolveGradeFromYear(year));
                })
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
        list.getBooks().addAll(loadBooks(dto.getBookIds()));

        replaceAssignmentTarget(list, dto, currentUser);

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

        clearAssignmentTarget(list);

        return readingListRepository.save(list);
    }

    @Transactional(readOnly = true)
    public List<Long> getBookIds(Long readingListId) {
        return readingListRepository.findByIdWithBooks(readingListId)
                .orElseThrow(() -> new EntityNotFoundException("Leeslijst niet gevonden"))
                .getBooks().stream()
                .map(BookEntity::getId)
                .toList();
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

        clearAssignmentTarget(list);

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

        replaceAssignmentTarget(list, dto, currentUser);

        return readingListRepository.save(list);
    }

    // region Helper methods

    private UserEntity requireCurrentUser(String smartschoolUid) {
        return userRepository.findDetailedBySmartschoolUid(smartschoolUid)
                .orElseGet(() -> userRepository.findBySmartschoolUid(smartschoolUid)
                        .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found")));
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

    private Long requireSchoolId(UserEntity user) {
        if (user.getSchool() == null || user.getSchool().getId() == null) {
            throw new IllegalArgumentException("Gebruiker heeft geen school");
        }

        return user.getSchool().getId();
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

    private void clearAssignmentTarget(ReadingListEntity list) {
        list.setTargetType(null);
        list.getTargetStudents().clear();
        list.getTargetClasses().clear();
        list.getTargetYears().clear();
        list.getTargetGrades().clear();
        list.setTargetAllSchools(false);
    }

    private void replaceAssignmentTarget(ReadingListEntity list, CreateReadingListDTO dto, UserEntity currentUser) {
        ReadingListTargetType targetType = dto.getTargetType();

        if (targetType == null) {
            throw new IllegalArgumentException("Kies een doelgroeptype voor deze klasleeslijst");
        }

        validateSingleTargetPayload(dto, targetType);

        clearAssignmentTarget(list);
        list.setTargetType(targetType);

        switch (targetType) {
            case STUDENTS -> {
                Set<UserEntity> students = loadTargetStudents(dto.getTargetStudentIds(), currentUser);

                if (students.isEmpty()) {
                    throw new IllegalArgumentException("Kies minstens één leerling als doelgroep");
                }

                list.getTargetStudents().addAll(students);
                list.setTargetAllSchools(false);
            }

            case CLASSES -> {
                Set<SchoolClassEntity> classes = loadTargetClasses(dto.getTargetClassIds(), currentUser);

                if (classes.isEmpty()) {
                    throw new IllegalArgumentException("Kies minstens één klas als doelgroep");
                }

                list.getTargetClasses().addAll(classes);
                list.setTargetAllSchools(false);
            }

            case YEARS -> {
                Set<Integer> years = normalizeYears(dto.getTargetYears());

                if (years.isEmpty()) {
                    throw new IllegalArgumentException("Kies minstens één jaar als doelgroep");
                }

                list.getTargetYears().addAll(years);
                list.setTargetAllSchools(Boolean.TRUE.equals(dto.getTargetAllSchools()));
            }

            case GRADES -> {
                Set<Integer> grades = normalizeGrades(dto.getTargetGrades());

                if (grades.isEmpty()) {
                    throw new IllegalArgumentException("Kies minstens één graad als doelgroep");
                }

                list.getTargetGrades().addAll(grades);
                list.setTargetAllSchools(Boolean.TRUE.equals(dto.getTargetAllSchools()));
            }
        }
    }

    private void validateSingleTargetPayload(CreateReadingListDTO dto, ReadingListTargetType targetType) {
        boolean hasStudentTargets = hasValues(dto.getTargetStudentIds());
        boolean hasClassTargets = hasValues(dto.getTargetClassIds());
        boolean hasYearTargets = hasValues(dto.getTargetYears());
        boolean hasGradeTargets = hasValues(dto.getTargetGrades());

        if (targetType != ReadingListTargetType.STUDENTS && hasStudentTargets) {
            throw new IllegalArgumentException("Leerlingen kunnen alleen gebruikt worden bij doelgroeptype STUDENTS");
        }

        if (targetType != ReadingListTargetType.CLASSES && hasClassTargets) {
            throw new IllegalArgumentException("Klassen kunnen alleen gebruikt worden bij doelgroeptype CLASSES");
        }

        if (targetType != ReadingListTargetType.YEARS && hasYearTargets) {
            throw new IllegalArgumentException("Jaren kunnen alleen gebruikt worden bij doelgroeptype YEARS");
        }

        if (targetType != ReadingListTargetType.GRADES && hasGradeTargets) {
            throw new IllegalArgumentException("Graden kunnen alleen gebruikt worden bij doelgroeptype GRADES");
        }

        if ((targetType == ReadingListTargetType.STUDENTS || targetType == ReadingListTargetType.CLASSES)
                && Boolean.TRUE.equals(dto.getTargetAllSchools())) {
            throw new IllegalArgumentException("Alle scholen kan alleen gebruikt worden bij jaren of graden");
        }
    }

    private boolean hasValues(Collection<?> values) {
        return values != null && values.stream().anyMatch(Objects::nonNull);
    }

    private Set<UserEntity> loadTargetStudents(List<Long> targetStudentIds, UserEntity currentUser) {
        if (targetStudentIds == null || targetStudentIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Long> sanitizedIds = targetStudentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UserEntity> users = userRepository.findAllById(sanitizedIds);

        if (users.size() != sanitizedIds.size()) {
            throw new IllegalArgumentException("Een of meerdere doelleerlingen bestaan niet");
        }

        Long actorSchoolId = requireSchoolId(currentUser);

        for (UserEntity user : users) {
            if (user.getRole() != UserRoles.STUDENT) {
                throw new IllegalArgumentException("Je kan alleen leerlingen als specifieke doelgroep kiezen");
            }

            Long studentSchoolId = requireSchoolId(user);

            if (!Objects.equals(actorSchoolId, studentSchoolId)) {
                throw new AccessDeniedException("Je kan alleen leerlingen van je eigen school gebruiken");
            }
        }

        return new LinkedHashSet<>(users);
    }

    private Set<SchoolClassEntity> loadTargetClasses(List<Long> targetClassIds, UserEntity currentUser) {
        if (targetClassIds == null || targetClassIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Long> sanitizedIds = targetClassIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<SchoolClassEntity> classes = schoolClassRepository.findAllById(sanitizedIds);

        if (classes.size() != sanitizedIds.size()) {
            throw new IllegalArgumentException("Een of meerdere doelklassen bestaan niet");
        }

        Long actorSchoolId = requireSchoolId(currentUser);

        for (SchoolClassEntity schoolClass : classes) {
            if (schoolClass.getSchool() == null || schoolClass.getSchool().getId() == null) {
                throw new IllegalArgumentException("Doelklas heeft geen school");
            }

            Long classSchoolId = schoolClass.getSchool().getId();

            if (!Objects.equals(actorSchoolId, classSchoolId)) {
                throw new AccessDeniedException("Je kan alleen klassen van je eigen school gebruiken");
            }
        }

        return new LinkedHashSet<>(classes);
    }

    private Set<Integer> normalizeYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return new TreeSet<>();
        }

        TreeSet<Integer> normalized = new TreeSet<>();

        for (Integer year : years) {
            if (year == null) {
                continue;
            }

            if (!VALID_YEARS.contains(year)) {
                throw new IllegalArgumentException("Ongeldig jaar: " + year);
            }

            normalized.add(year);
        }

        return normalized;
    }

    private Set<Integer> normalizeGrades(List<Integer> grades) {
        if (grades == null || grades.isEmpty()) {
            return new TreeSet<>();
        }

        TreeSet<Integer> normalized = new TreeSet<>();

        for (Integer grade : grades) {
            if (grade == null) {
                continue;
            }

            if (!VALID_GRADES.contains(grade)) {
                throw new IllegalArgumentException("Ongeldige graad: " + grade);
            }

            normalized.add(grade);
        }

        return normalized;
    }

    private boolean canViewClassList(ReadingListEntity list, UserEntity currentUser) {
        if (list.getListType() != ReadingListType.CLASS) {
            return false;
        }

        if (Objects.equals(list.getCreator().getId(), currentUser.getId())) {
            return true;
        }

        return matchesAssignmentTarget(list, currentUser);
    }

    private boolean matchesAssignmentTarget(ReadingListEntity list, UserEntity user) {
        if (user.getRole() != UserRoles.STUDENT) {
            return false;
        }

        ReadingListTargetType targetType = list.getTargetType();

        if (targetType == null) {
            return false;
        }

        return switch (targetType) {
            case STUDENTS -> matchesTargetStudent(list, user);
            case CLASSES -> matchesTargetClass(list, user);
            case YEARS -> matchesTargetYear(list, user);
            case GRADES -> matchesTargetGrade(list, user);
        };
    }

    private boolean matchesTargetStudent(ReadingListEntity list, UserEntity user) {
        return list.getTargetStudents().stream()
                .map(UserEntity::getId)
                .filter(Objects::nonNull)
                .anyMatch(targetStudentId -> Objects.equals(targetStudentId, user.getId()));
    }

    private boolean matchesTargetClass(ReadingListEntity list, UserEntity user) {
        Set<Long> userClassIds = user.getClasses().stream()
                .map(SchoolClassEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return list.getTargetClasses().stream()
                .map(SchoolClassEntity::getId)
                .filter(Objects::nonNull)
                .anyMatch(userClassIds::contains);
    }

    private boolean matchesTargetYear(ReadingListEntity list, UserEntity user) {
        if (list.getTargetYears().isEmpty()) {
            return false;
        }

        boolean schoolAllowed = list.isTargetAllSchools() || isSameSchool(list.getCreator(), user);

        if (!schoolAllowed) {
            return false;
        }

        Set<Integer> userYears = resolveUserYears(user);

        return list.getTargetYears().stream().anyMatch(userYears::contains);
    }

    private boolean matchesTargetGrade(ReadingListEntity list, UserEntity user) {
        if (list.getTargetGrades().isEmpty()) {
            return false;
        }

        boolean schoolAllowed = list.isTargetAllSchools() || isSameSchool(list.getCreator(), user);

        if (!schoolAllowed) {
            return false;
        }

        Set<Integer> userGrades = resolveUserYears(user).stream()
                .map(this::resolveGradeFromYear)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return list.getTargetGrades().stream().anyMatch(userGrades::contains);
    }

    private Set<Integer> resolveUserYears(UserEntity user) {
        return user.getClasses().stream()
                .map(SchoolClassEntity::getName)
                .map(this::resolveYearFromClassName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean canSeeLocalTargetDetails(ReadingListEntity list, UserEntity currentUser) {
        if (list == null || currentUser == null) {
            return false;
        }

        if (list.getCreator() != null
                && Objects.equals(list.getCreator().getId(), currentUser.getId())) {
            return true;
        }

        return isStaffRole(currentUser.getRole())
                && isSameSchool(list.getCreator(), currentUser);
    }

    private boolean isSameSchool(UserEntity left, UserEntity right) {
        if (left == null || right == null || left.getSchool() == null || right.getSchool() == null) {
            return false;
        }

        return Objects.equals(left.getSchool().getId(), right.getSchool().getId());
    }

    private Integer resolveYearFromClassName(String className) {
        if (className == null || className.trim().isBlank()) {
            return null;
        }

        char first = className.trim().charAt(0);

        if (!Character.isDigit(first)) {
            return null;
        }

        int year = Character.getNumericValue(first);

        return VALID_YEARS.contains(year) ? year : null;
    }

    private Integer resolveGradeFromYear(Integer year) {
        if (year == null) {
            return null;
        }

        if (year == 1 || year == 2) {
            return 1;
        }

        if (year == 3 || year == 4) {
            return 2;
        }

        if (year == 5 || year == 6 || year == 7) {
            return 3;
        }

        return null;
    }

    private List<String> collectRelevantUserUids(Collection<ReadingListEntity> lists, UserEntity currentUser) {
        LinkedHashSet<String> uids = new LinkedHashSet<>();

        for (ReadingListEntity list : lists) {
            if (canSeeCreatorLiveName(list, currentUser)) {
                addUid(uids, list.getCreator().getSmartschoolUid());
            }

            if (canSeeLocalTargetDetails(list, currentUser)) {
                list.getTargetStudents()
                        .forEach(student -> addUid(uids, student.getSmartschoolUid()));
            }
        }

        return new ArrayList<>(uids);
    }

    private boolean canSeeCreatorLiveName(ReadingListEntity list, UserEntity currentUser) {
        if (list == null || currentUser == null || list.getCreator() == null) {
            return false;
        }

        if (Objects.equals(list.getCreator().getId(), currentUser.getId())) {
            return true;
        }

        return isSameSchool(list.getCreator(), currentUser);
    }

    private void addUid(Set<String> uids, String uid) {
        if (uid == null || uid.trim().isBlank()) {
            return;
        }

        uids.add(uid.trim());
    }

    private Map<String, String> resolveDisplayNamesMap(String actorUid, List<String> uids) {
        if (uids == null || uids.isEmpty()) {
            return Map.of();
        }

        try {
            var response = userDirectoryService.resolveDisplayNames(
                    actorUid,
                    new ResolveDisplayNamesRequest(uids, null));

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

    private String fallbackStudentLabel(UserEntity user) {
        return "Leerling #" + user.getId();
    }

    private String resolveStudentDisplayName(UserEntity student, Map<String, String> displayNames) {
        String studentUid = student.getSmartschoolUid();

        if (studentUid == null || studentUid.isBlank()) {
            return fallbackStudentLabel(student);
        }

        return displayNames.getOrDefault(studentUid, fallbackStudentLabel(student));
    }

    private ReadingListOverviewDTO toOverview(
            ReadingListEntity list,
            UserEntity currentUser,
            Map<String, String> displayNames) {
        boolean showLocalTargetDetails = canSeeLocalTargetDetails(list, currentUser);

        return new ReadingListOverviewDTO(
                list.getId(),
                list.getPublicUid(),
                list.getTitle(),
                list.getTaskDescription(),
                list.getDeadline(),
                resolveCreatorName(list, displayNames),
                list.getBooks().size(),
                list.getBooks().stream().map(BookEntity::getId).toList(),
                list.getListType(),
                Objects.equals(list.getCreator().getId(), currentUser.getId()),
                list.isPublicVisible(),
                showLocalTargetDetails ? list.getTargetType() : null,
                showLocalTargetDetails ? targetStudentDisplayNames(list, displayNames) : List.of(),
                showLocalTargetDetails ? targetClassNames(list) : List.of(),
                showLocalTargetDetails ? sortedIntegers(list.getTargetYears()) : List.of(),
                showLocalTargetDetails ? sortedIntegers(list.getTargetGrades()) : List.of(),
                showLocalTargetDetails && list.isTargetAllSchools());
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

    private ReadingListDetailDTO toDetail(
            ReadingListEntity list,
            UserEntity currentUser,
            Map<String, String> displayNames) {
        boolean showLocalTargetDetails = canSeeLocalTargetDetails(list, currentUser);

        List<ReadingListBookDTO> books = list.getBooks().stream()
                .map(book -> {
                    List<String> authors = book.getAuthors() == null
                            ? List.of()
                            : List.copyOf(book.getAuthors());

                    return new ReadingListBookDTO(
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
                list.getPublicUid(),
                list.getTitle(),
                list.getTaskDescription(),
                list.getDeadline(),
                list.getListType(),
                Objects.equals(list.getCreator().getId(), currentUser.getId()),
                list.isPublicVisible(),
                resolveCreatorName(list, displayNames),
                showLocalTargetDetails ? list.getTargetType() : null,
                showLocalTargetDetails ? targetStudentIds(list) : List.of(),
                showLocalTargetDetails ? targetStudentDisplayNames(list, displayNames) : List.of(),
                showLocalTargetDetails ? targetStudents(list, displayNames) : List.of(),
                showLocalTargetDetails ? targetClassIds(list) : List.of(),
                showLocalTargetDetails ? targetClassNames(list) : List.of(),
                showLocalTargetDetails ? sortedIntegers(list.getTargetYears()) : List.of(),
                showLocalTargetDetails ? sortedIntegers(list.getTargetGrades()) : List.of(),
                showLocalTargetDetails && list.isTargetAllSchools(),
                books);
    }

    private PublicReadingListDetailDTO toPublicDetailDTO(ReadingListEntity list) {
        return new PublicReadingListDetailDTO(
                list.getPublicUid(),
                list.getTitle(),
                list.getTaskDescription(),
                list.getDeadline(),
                list.getCreator() == null || list.getCreator().getRole() == null ? null
                        : list.getCreator().getRole().name(),
                toBookDTOs(list));
    }

    private List<ReadingListBookDTO> toBookDTOs(ReadingListEntity list) {
        if (list.getBooks() == null) {
            return List.of();
        }

        return list.getBooks()
                .stream()
                .sorted(Comparator.comparing(BookEntity::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(book -> {
                    List<String> authors = book.getAuthors() == null
                            ? List.of()
                            : List.copyOf(book.getAuthors());

                    return new ReadingListBookDTO(
                            book.getId(),
                            book.getTitle(),
                            authors,
                            book.getThumbnail(),
                            book.getIsbn(),
                            book.getAvailableCopies() != null ? book.getAvailableCopies() : 0);
                })
                .toList();
    }

    private List<Long> targetStudentIds(ReadingListEntity list) {
        return list.getTargetStudents().stream()
                .map(UserEntity::getId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private List<ReadingListAssignmentTargetsDTO.StudentTarget> targetStudents(
            ReadingListEntity list,
            Map<String, String> displayNames) {
        return list.getTargetStudents().stream()
                .map(student -> new ReadingListAssignmentTargetsDTO.StudentTarget(
                        student.getId(),
                        resolveStudentDisplayName(student, displayNames),
                        sortedClassNames(student.getClasses())))
                .sorted(Comparator.comparing(
                        ReadingListAssignmentTargetsDTO.StudentTarget::displayName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<String> targetStudentDisplayNames(ReadingListEntity list, Map<String, String> displayNames) {
        return list.getTargetStudents().stream()
                .map(student -> resolveStudentDisplayName(student, displayNames))
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<Long> targetClassIds(ReadingListEntity list) {
        return list.getTargetClasses().stream()
                .map(SchoolClassEntity::getId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private List<String> targetClassNames(ReadingListEntity list) {
        return list.getTargetClasses().stream()
                .map(SchoolClassEntity::getName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> sortedClassNames(Set<SchoolClassEntity> classes) {
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }

        return classes.stream()
                .map(SchoolClassEntity::getName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<Integer> sortedIntegers(Set<Integer> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private boolean matchesStudentSearch(
            UserEntity student,
            String displayName,
            List<String> classNames,
            String normalizedQuery) {
        return containsNormalized(displayName, normalizedQuery)
                || containsNormalized(student.getSmartschoolUid(), normalizedQuery)
                || classNames.stream().anyMatch(className -> containsNormalized(className, normalizedQuery));
    }

    private boolean containsNormalized(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
    // endregion

}