package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.*;
import edu.ap.gosmartlib.dto.googlebooks.GoogleBooksResponse;
import edu.ap.gosmartlib.dto.googlebooks.VolumeInfo;
import edu.ap.gosmartlib.entities.book.BookCopyEntity;
import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.book.BookInventoryEntity;
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.exceptions.NegativeValueException;
import edu.ap.gosmartlib.repositories.book.BookCopyRepository;
import edu.ap.gosmartlib.repositories.book.BookInventoryRepository;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import edu.ap.gosmartlib.util.BookCopyCondition;
import org.springframework.data.domain.*;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import edu.ap.gosmartlib.dto.importdto.BulkImportDuplicateWarningDTO;
import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.dto.importdto.ImportMismatchDTO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final BookFilterValidator bookFilterValidator;
    private final SchoolRepository schoolRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookInventoryRepository bookInventoryRepository;

    private final InventoryAdjustmentService inventoryAdjustmentService;

    @Value("${google.books.api.url}")
    private String googleBooksApiUrl;

    @Value("${google.books.api.key}")
    private String googleBooksApiKey;

    public Page<BookDTO> getAllBooks(int page, int size, UserRoles callerRole, String currentUserUid) {
        if (page < 0 || size <= 0)
            throw new NegativeValueException("Page number cannot be negative and size must be greater than 0");

        Pageable pageable = PageRequest.of(page, size);
        boolean includeDidactic = canSeeDidactic(callerRole);
        if (restrictToOwnSchool(callerRole)) {
            Long schoolId = requireRequesterSchoolId(currentUserUid);

            return bookRepository.findAllFilteredForSchool(includeDidactic, schoolId, pageable)
                    .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid));
        }

        return bookRepository.findAllFiltered(includeDidactic, pageable)
                .map(this::toDTO);
    }

    public List<BookDTO> getAllBooksUnpaged(UserRoles callerRole, String currentUserUid) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        return bookRepository.findAll()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid))
                .filter(Objects::nonNull)
                .toList();
    }

    public BookDTO searchBookByIsbn(String isbn) {
        BookEntity previewBook = buildBookEntityFromGoogle(isbn);
        previewBook.setInventories(new ArrayList<>());
        previewBook.setTotalCopies(0);
        previewBook.setAvailableCopies(0);
        return toDTO(previewBook);
    }

    // AANGEPAST: 4 argumenten (inclusief Integer copies), passend bij de
    // BookController
    public BookDTO addBookByIsbn(String isbn, String smartschoolUid, String campus, Integer copies) {
        BookEntity newBook = buildBookEntityFromGoogle(isbn);

        // Zorg dat er altijd minimaal 1 copy is als er null wordt meegegeven
        int totalCopies = (copies != null && copies > 0) ? copies : 1;

        applySingleInventoryForCurrentUser(newBook, smartschoolUid, campus, totalCopies, totalCopies);
        recomputeBookCopyTotals(newBook);
        BookEntity savedBook = bookRepository.save(newBook);
        savedBook.getInventories().forEach(this::reconcileCopiesForInventory);
        return toDTO(savedBook);
    }

    private Integer extractYear(String publishedDate) {
        if (publishedDate != null && publishedDate.length() >= 4) {
            try {
                return Integer.parseInt(publishedDate.substring(0, 4));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public BookDTO getBookById(Long id, UserRoles callerRole, String currentUserUid) throws BookNotFoundException {

        BookEntity book = bookRepository.findDetailedById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        if (book.isDidacticTag() && !canSeeDidactic(callerRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Je hebt geen toegang tot dit boek");
        }
        BookDTO visible = toVisibleBookDTO(book, callerRole, currentUserUid);
        if (visible == null) {
            throw new BookNotFoundException(id);
        }

        return visible;
    }

    public void updateSpotlight(Long id, boolean spotlight) {
        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        book.setSpotlight(spotlight);
        bookRepository.save(book);
    }

    public Page<BookDTO> searchByTitleOrAuthorOrCategory(String query, int page, int size, UserRoles callerRole,
            String currentUserUid) {
        if (page < 0 || size <= 0)
            throw new NegativeValueException("Page number cannot be negative and size must be greater than 0");

        Pageable pageable = PageRequest.of(page, size);
        boolean includeDidactic = canSeeDidactic(callerRole);

        if (restrictToOwnSchool(callerRole)) {
            Long schoolId = requireRequesterSchoolId(currentUserUid);

            if (query == null || query.isBlank()) {
                return bookRepository.findAllFilteredForSchool(includeDidactic, schoolId, pageable)
                        .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid));
            }

            Page<BookEntity> raw = bookRepository.searchByTitleOrAuthorOrCategoryForSchool(
                    query.trim(), includeDidactic, schoolId, pageable);
            return prioritizeTitleMatches(raw, query.trim(), pageable,
                    book -> toVisibleBookDTO(book, callerRole, currentUserUid));
        }

        if (query == null || query.isBlank()) {
            return bookRepository.findAllFiltered(canSeeDidactic(callerRole), pageable).map(this::toDTO);
        }

        Page<BookEntity> raw = bookRepository.searchByTitleOrAuthorOrCategory(query.trim(), includeDidactic, pageable);
        return prioritizeTitleMatches(raw, query.trim(), pageable, this::toDTO);
    }

    private Page<BookDTO> prioritizeTitleMatches(Page<BookEntity> raw, String query, Pageable pageable,
            Function<BookEntity, BookDTO> mapper) {
        String lowerQuery = query.toLowerCase();
        List<BookDTO> sorted = raw.getContent().stream()
                .sorted(Comparator.comparingInt(b -> b.getTitle().toLowerCase().contains(lowerQuery) ? 0 : 1))
                .map(mapper)
                .filter(Objects::nonNull)
                .toList();
        return new PageImpl<>(sorted, pageable, raw.getTotalElements());
    }

    public List<BookDTO> getTop4BooksInSpotlight(UserRoles callerRole, String currentUserUid) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        return bookRepository.findTop4BySpotlightTrueOrderByIdDesc()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<BookDTO> getTop4BooksInSpotlight(UserRoles callerRole, String currentUserUid, String readingLevel) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        Long schoolId = restrictToOwnSchool(callerRole) ? requireRequesterSchoolId(currentUserUid) : null;
        String normalizedReadingLevel = safeTrim(readingLevel);

        if (normalizedReadingLevel != null && normalizedReadingLevel.isBlank()) {
            normalizedReadingLevel = null;
        }

        return bookRepository.findSpotlightBooksByReadingLevel(
                normalizedReadingLevel,
                includeDidactic,
                schoolId,
                PageRequest.of(0, 4))
                .stream()
                .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<BookDTO> getAllBooksInSpotlight(UserRoles callerRole, String currentUserUid) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        return bookRepository.findBySpotlightTrueOrderByIdDesc()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<BookDTO> getLatestBooks(UserRoles callerRole, String currentUserUid) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        return bookRepository.findTop4ByOrderByIdDesc()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<BookDTO> getLatestBooks(UserRoles callerRole, String currentUserUid, String readingLevel) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        Long schoolId = restrictToOwnSchool(callerRole) ? requireRequesterSchoolId(currentUserUid) : null;
        String normalizedReadingLevel = safeTrim(readingLevel);

        if (normalizedReadingLevel != null && normalizedReadingLevel.isBlank()) {
            normalizedReadingLevel = null;
        }

        return bookRepository.findLatestBooksByReadingLevel(
                normalizedReadingLevel,
                includeDidactic,
                schoolId,
                PageRequest.of(0, 4))
                .stream()
                .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookDTO> getRecommendedBooksForUser(String smartschoolUid) {
        UserEntity user = userRepository.findDetailedBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker niet gevonden"));

        List<BookEntity> books;

        if (user.getRole() == UserRoles.TEACHER) {
            books = bookRepository.findTop4ByDidacticTagTrueOrderByRatingDesc();
        } else if (user.getRole() == UserRoles.STUDENT) {
            String ageRange = determineAgeRangeFromStudentClass(user);
            books = bookRepository.findTop4ByAgeRangeIgnoreCaseAndDidacticTagFalseOrderByRatingDesc(ageRange);
        } else {
            books = bookRepository.findTop4ByOrderByRatingDesc();
        }

        return books.stream()
                .map(book -> toVisibleBookDTO(book, user.getRole(), smartschoolUid))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookDTO> getTopRatedBooksByReadingLevel(
            UserRoles callerRole,
            String currentUserUid,
            String readingLevel) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        Long schoolId = restrictToOwnSchool(callerRole) ? requireRequesterSchoolId(currentUserUid) : null;
        String normalizedReadingLevel = safeTrim(readingLevel);

        if (normalizedReadingLevel != null && normalizedReadingLevel.isBlank()) {
            normalizedReadingLevel = null;
        }

        return bookRepository.findTopRatedBooksByReadingLevel(
                normalizedReadingLevel,
                includeDidactic,
                schoolId,
                PageRequest.of(0, 4))
                .stream()
                .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid))
                .filter(Objects::nonNull)
                .toList();
    }

    public Page<BookDTO> filterBooks(BookFilterRequest request, UserRoles callerRole, String currentUserUid) {
        bookFilterValidator.validate(request);

        Pageable pageable = PageRequest.of(request.page(), request.size(), resolveSort(request.sortBy()));
        boolean includeDidactic = canSeeDidactic(callerRole);
        boolean useRelevance = request.query() != null && !request.query().isBlank()
                && (request.sortBy() == null || request.sortBy().isBlank() || "default".equals(request.sortBy()));

        if (restrictToOwnSchool(callerRole)) {
            Long schoolId = requireRequesterSchoolId(currentUserUid);
            Page<BookEntity> raw = bookRepository.filterBooksForSchool(
                    includeDidactic,
                    request.query(),
                    request.language(),
                    request.categories(),
                    request.labels(),
                    request.readingLevel(),
                    request.minPageCount(),
                    request.maxPageCount(),
                    request.minPubYear(),
                    request.maxPubYear(),
                    request.minRating(),
                    request.maxRating(),
                    schoolId,
                    pageable);
            if (useRelevance) {
                return prioritizeTitleMatches(raw, request.query().trim(), pageable,
                        book -> toVisibleBookDTO(book, callerRole, currentUserUid));
            }
            return raw.map(book -> toVisibleBookDTO(book, callerRole, currentUserUid));
        }

        Page<BookEntity> raw = bookRepository.filterBooks(
                includeDidactic,
                request.query(),
                request.language(),
                request.categories(),
                request.labels(),
                request.readingLevel(),
                Boolean.TRUE.equals(request.didacticOnly()),// null-safe unbox moet erbij anders leerkrachtenpad geeft errors
                request.minPageCount(),
                request.maxPageCount(),
                request.minPubYear(),
                request.maxPubYear(),
                request.minRating(),
                request.maxRating(),
                pageable);
        if (useRelevance) {
            return prioritizeTitleMatches(raw, request.query().trim(), pageable, this::toDTO);
        }
        return raw.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableLanguages(String currentUserUid) {
        Long schoolId = currentUserUid == null || currentUserUid.isBlank()
                ? null
                : getRequesterSchoolId(currentUserUid);

        List<String> rawLanguages = schoolId == null
                ? bookRepository.findDistinctLanguages()
                : bookRepository.findDistinctLanguagesForSchool(schoolId);

        List<String> cleanedLanguages = rawLanguages.stream()
                .map(this::safeTrim)
                .filter(language -> language != null && !language.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> availableLanguages = new ArrayList<>();
        Set<String> seenLanguages = new HashSet<>();

        for (String language : cleanedLanguages) {
            if (seenLanguages.add(language.toLowerCase(Locale.ROOT))) {
                availableLanguages.add(language);
            }
        }

        return availableLanguages;
    }

    public BulkImportResponseDTO importBooksFromExcel(MultipartFile file, String smartschoolUid, String campus) {
        return importBooksFromExcel(file, smartschoolUid, campus, false, List.of());
    }

    public BulkImportResponseDTO importBooksFromExcel(
            MultipartFile file,
            String smartschoolUid,
            String fallbackCampus,
            boolean confirmDuplicates,
            List<Integer> confirmedDuplicateRows) {

        Set<Integer> confirmedDuplicateRowSet = confirmedDuplicateRows == null
                ? Set.of()
                : new HashSet<>(confirmedDuplicateRows);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload een excel file die niet leeg is");
        }

        SchoolEntity userSchool = resolveSchoolForUser(smartschoolUid);

        List<ImportMismatchDTO> mismatches = new ArrayList<>();
        List<BulkImportDuplicateWarningDTO> duplicateWarnings = new ArrayList<>();
        int totalRows = 0;
        int savedCount = 0;

        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("Boekenlijst");

            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columns = getImportColumnIndexes(headerRow, formatter);

            if (!columns.containsKey("isbn")) {
                throw new IllegalArgumentException("Kolom 'ISBN' ontbreekt in het Excelbestand");
            }

            if (!columns.containsKey("titel")) {
                throw new IllegalArgumentException("Kolom 'Titel' ontbreekt in het Excelbestand");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || rowIsBlank(row, formatter)) {
                    continue;
                }

                String isbn = getImportCell(row, columns, formatter, "isbn");
                String excelTitle = getImportCell(row, columns, formatter, "titel");
                int rowNumber = row.getRowNum() + 1;

                totalRows++;

                if (isbn.isBlank() || excelTitle.isBlank()) {
                    mismatches.add(new ImportMismatchDTO(
                            rowNumber,
                            isbn,
                            excelTitle,
                            null,
                            "Geen ISBN nummer of titel"));
                    continue;
                }

                try {
                    String categories = getImportCell(row, columns, formatter, "categorieën");
                    String labels = getImportCell(row, columns, formatter, "leefwereldlabels");
                    String readingLevel = getImportCell(row, columns, formatter, "leesniveau");
                    String rowCampus = getImportCell(row, columns, formatter, "campus");
                    String totalCopiesValue = getImportCell(row, columns, formatter, "totaal aantal boeken");
                    String availableCopiesValue = getImportCell(row, columns, formatter, "beschikbaar aantal boeken");

                    int totalCopies = parsePositiveIntOrDefault(
                            totalCopiesValue,
                            1,
                            "Totaal aantal boeken");
                    int availableCopies = parseNonNegativeIntOrDefault(
                            availableCopiesValue,
                            totalCopies,
                            "Beschikbaar aantal boeken");

                    validateInventoryCounts(totalCopies, availableCopies);

                    String campusToUse = !rowCampus.isBlank()
                            ? rowCampus
                            : fallbackCampus;
                    String normalizedCampus = normalizeCampus(campusToUse);

                    BookEntity duplicateBook = bookRepository.findByNormalizedIsbn(isbn)
                            .orElse(null);

                    if (duplicateBook != null) {
                        BookInventoryEntity affectedInventory = findInventory(
                                duplicateBook,
                                userSchool,
                                normalizedCampus);

                        if (!confirmDuplicates) {
                            duplicateWarnings.add(new BulkImportDuplicateWarningDTO(
                                    rowNumber,
                                    duplicateBook.getId(),
                                    duplicateBook.getTitle(),
                                    cleanStringList(duplicateBook.getAuthors()),
                                    duplicateBook.getPublisher(),
                                    normalizedCampus,
                                    totalCopies,
                                    availableCopies,
                                    affectedInventory == null ? 0 : safeCopyCount(affectedInventory.getTotalCopies()),
                                    affectedInventory == null ? 0
                                            : safeCopyCount(affectedInventory.getAvailableCopies()),
                                    affectedInventory == null
                                            ? "Dit ISBN bestaat al in de database. Er zou een nieuwe campusvoorraad worden toegevoegd."
                                            : "Dit ISBN bestaat al op deze campus. De aantallen zouden verhoogd worden."));

                            continue;
                        }

                        if (!confirmedDuplicateRowSet.contains(rowNumber)) {
                            continue;
                        }

                        addOrIncreaseInventory(
                                duplicateBook,
                                userSchool,
                                normalizedCampus,
                                totalCopies,
                                availableCopies);

                        savedCount++;
                        continue;
                    }

                    if (!confirmDuplicates) {
                        continue;
                    }

                    BookEntity fetchedBook = buildBookEntityFromGoogle(isbn);

                    if (!titlesMatch(excelTitle, fetchedBook.getTitle())) {
                        mismatches.add(new ImportMismatchDTO(
                                rowNumber,
                                isbn,
                                excelTitle,
                                fetchedBook.getTitle(),
                                "De titel komt niet overeen (" + fetchedBook.getTitle() + ")",
                                totalCopies));
                        continue;
                    }

                    applyBulkIsbnImportFields(fetchedBook, categories, labels, readingLevel);
                    fetchedBook.getInventories().clear();
                    addInventory(fetchedBook, userSchool, normalizedCampus, totalCopies, availableCopies);
                    recomputeBookCopyTotals(fetchedBook);

                    BookEntity savedFetched = bookRepository.save(fetchedBook);
                    savedFetched.getInventories().forEach(this::reconcileCopiesForInventory);
                    savedCount++;

                } catch (IllegalArgumentException e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowNumber,
                            isbn,
                            excelTitle,
                            null,
                            e.getMessage()));
                } catch (Exception e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowNumber,
                            isbn,
                            excelTitle,
                            null,
                            "Onverwachte fout bij het ophalen en opslaan"));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Kon de excel file niet lezen", e);
        }

        if (!confirmDuplicates) {
            if (!duplicateWarnings.isEmpty()) {
                return new BulkImportResponseDTO(
                        totalRows,
                        0,
                        mismatches.size(),
                        mismatches,
                        duplicateWarnings.size(),
                        duplicateWarnings);
            }

            return importBooksFromExcel(
                    file,
                    smartschoolUid,
                    fallbackCampus,
                    true,
                    List.of());
        }

        return new BulkImportResponseDTO(
                totalRows,
                savedCount,
                mismatches.size(),
                mismatches,
                duplicateWarnings.size(),
                duplicateWarnings);
    }

    public BulkImportResponseDTO importBooksWithoutIsbnFromExcel(
            MultipartFile file,
            String smartschoolUid,
            String fallbackCampus,
            boolean confirmDuplicates,
            List<Integer> confirmedDuplicateRows) {

        Set<Integer> confirmedDuplicateRowSet = confirmedDuplicateRows == null
                ? Set.of()
                : new HashSet<>(confirmedDuplicateRows);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload een excel file die niet leeg is");
        }

        SchoolEntity userSchool = resolveSchoolForUser(smartschoolUid);

        List<ImportMismatchDTO> mismatches = new ArrayList<>();
        List<BulkImportDuplicateWarningDTO> duplicateWarnings = new ArrayList<>();
        int totalRows = 0;
        int savedCount = 0;

        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("Boekenlijst");

            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columns = getImportColumnIndexes(headerRow, formatter);

            if (!columns.containsKey("titel")) {
                throw new IllegalArgumentException("Kolom 'Titel' ontbreekt in het Excelbestand");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || rowIsBlank(row, formatter)) {
                    continue;
                }

                String title = getImportCell(row, columns, formatter, "titel");
                totalRows++;

                if (title.isBlank()) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            "",
                            "",
                            null,
                            "Titel is verplicht"));
                    continue;
                }

                try {
                    String authors = getImportCell(row, columns, formatter, "auteurs");
                    String publisher = getImportCell(row, columns, formatter, "uitgever");
                    String description = getImportCell(row, columns, formatter, "omschrijving");
                    String pageCount = getImportCell(row, columns, formatter, "aantal pagina's");
                    String categories = getImportCell(row, columns, formatter, "categorieën");
                    String labels = getImportCell(row, columns, formatter, "leefwereldlabels");
                    String thumbnail = getImportCell(row, columns, formatter, "fotourl");
                    String language = getImportCell(row, columns, formatter, "taal");
                    String publishedYear = getImportCell(row, columns, formatter, "jaar van uitgave");
                    String readingLevel = getImportCell(row, columns, formatter, "leesniveau");
                    String didacticBook = getImportCell(row, columns, formatter, "didactisch boek");
                    String rowCampus = getImportCell(row, columns, formatter, "campus");
                    String totalCopiesValue = getImportCell(row, columns, formatter, "totaal aantal boeken");
                    String availableCopiesValue = getImportCell(row, columns, formatter, "beschikbaar aantal boeken");

                    int totalCopies = parsePositiveIntOrDefault(
                            totalCopiesValue,
                            1,
                            "Totaal aantal boeken");
                    int availableCopies = parseNonNegativeIntOrDefault(availableCopiesValue, totalCopies,
                            "Beschikbaar aantal boeken");

                    validateInventoryCounts(totalCopies, availableCopies);

                    List<String> parsedAuthors = splitImportList(authors);
                    String normalizedPublisher = valueOrFallback(publisher, "Onbekende uitgever");

                    String campusToUse = !rowCampus.isBlank()
                            ? rowCampus
                            : fallbackCampus;

                    String normalizedCampus = normalizeCampus(campusToUse);

                    BookEntity duplicateBook = findDuplicateBookForNoIsbnImport(
                            title,
                            parsedAuthors,
                            normalizedPublisher,
                            userSchool);

                    if (duplicateBook != null) {
                        int rowNumber = row.getRowNum() + 1;

                        BookInventoryEntity affectedInventory = findInventory(
                                duplicateBook,
                                userSchool,
                                normalizedCampus);

                        if (!confirmDuplicates) {
                            duplicateWarnings.add(new BulkImportDuplicateWarningDTO(
                                    rowNumber,
                                    duplicateBook.getId(),
                                    duplicateBook.getTitle(),
                                    cleanStringList(duplicateBook.getAuthors()),
                                    duplicateBook.getPublisher(),
                                    normalizedCampus,
                                    totalCopies,
                                    availableCopies,
                                    affectedInventory == null ? 0 : safeCopyCount(affectedInventory.getTotalCopies()),
                                    affectedInventory == null ? 0
                                            : safeCopyCount(affectedInventory.getAvailableCopies()),
                                    affectedInventory == null
                                            ? "Dit boek bestaat al op schoolniveau. Er zou een nieuwe campusvoorraad worden toegevoegd."
                                            : "Dit boek bestaat al op deze campus. De aantallen zouden verhoogd worden."));

                            continue;
                        }

                        if (!confirmedDuplicateRowSet.contains(rowNumber)) {
                            continue;
                        }

                        addOrIncreaseInventory(
                                duplicateBook,
                                userSchool,
                                normalizedCampus,
                                totalCopies,
                                availableCopies);

                        savedCount++;
                        continue;
                    }

                    if (!confirmDuplicates) {
                        continue;
                    }

                    BookEntity book = new BookEntity();
                    book.setTitle(title.trim());
                    book.setAuthors(parsedAuthors);
                    book.setPublisher(normalizedPublisher);
                    book.setDescription(valueOrFallback(description, "Geen omschrijving beschikbaar"));
                    book.setPageCount(parseNonNegativeIntOrDefault(pageCount, 0, "Aantal pagina's"));
                    book.setCategories(splitImportList(categories));
                    book.setLabels(splitImportList(labels));
                    book.setThumbnail(valueOrFallback(thumbnail, ""));
                    book.setLanguage(normalizeLanguage(language));
                    book.setRating(0.0);
                    book.setPublishedYear(parsePublishedYearOrNull(publishedYear));
                    book.setSpotlight(false);
                    book.setDidacticTag(parseDidacticBoolean(didacticBook));
                    book.setReadingLevel(normalizeReadingLevel(readingLevel));
                    book.setAgeRange(null);
                    book.setIsbn("NOISBN-" + java.util.UUID.randomUUID());

                    addInventory(
                            book,
                            userSchool,
                            normalizedCampus,
                            totalCopies,
                            availableCopies);

                    recomputeBookCopyTotals(book);
                    BookEntity savedNoIsbn = bookRepository.save(book);
                    savedNoIsbn.getInventories().forEach(this::reconcileCopiesForInventory);
                    savedCount++;

                } catch (Exception e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            "",
                            title,
                            null,
                            e.getMessage()));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Kon de excel file niet lezen", e);
        }

        if (!confirmDuplicates) {
            if (!duplicateWarnings.isEmpty()) {
                return new BulkImportResponseDTO(
                        totalRows,
                        0,
                        mismatches.size(),
                        mismatches,
                        duplicateWarnings.size(),
                        duplicateWarnings);
            }

            return importBooksWithoutIsbnFromExcel(
                    file,
                    smartschoolUid,
                    fallbackCampus,
                    true,
                    List.of());
        }

        return new BulkImportResponseDTO(
                totalRows,
                savedCount,
                mismatches.size(),
                mismatches,
                duplicateWarnings.size(),
                duplicateWarnings);
    }

    public BookDTO updateBook(Long id, BookDTO updatedBook) {
        BookEntity book = bookRepository.findDetailedById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        if (updatedBook.title() != null && updatedBook.title().isBlank())
            throw new IllegalArgumentException("Titel mag niet leeg zijn");

        if (updatedBook.pageCount() != null && updatedBook.pageCount() < 0)
            throw new IllegalArgumentException("Paginacount mag niet negatief zijn");
        if (updatedBook.publishedYear() != null && updatedBook.publishedYear() <= 0)
            throw new IllegalArgumentException("Publicatiejaar moet groter zijn dan 0");

        if (updatedBook.publishedYear() != null && updatedBook.publishedYear() > Year.now().getValue())
            throw new IllegalArgumentException("Publicatiejaar mag niet in de toekomst liggen");

        if (updatedBook.isbn() != null && !updatedBook.isbn().equals(book.getIsbn())
                && bookRepository.existsByIsbn(updatedBook.isbn())) {
            throw new IllegalArgumentException("ISBN bestaat al in de database");
        }

        if (updatedBook.isbn() != null && updatedBook.isbn().isBlank())
            throw new IllegalArgumentException("ISBN mag niet leeg zijn");

        if (updatedBook.title() != null)
            book.setTitle(updatedBook.title().trim());
        if (updatedBook.authors() != null)
            book.setAuthors(cleanStringList(updatedBook.authors()));
        if (updatedBook.publisher() != null)
            book.setPublisher(safeTrim(updatedBook.publisher()));
        if (updatedBook.description() != null)
            book.setDescription(safeTrim(updatedBook.description()));
        if (updatedBook.pageCount() != null)
            book.setPageCount(updatedBook.pageCount());
        if (updatedBook.categories() != null)
            book.setCategories(cleanStringList(updatedBook.categories()));
        if (updatedBook.labels() != null)
            book.setLabels(cleanStringList(updatedBook.labels()));
        if (updatedBook.thumbnail() != null)
            book.setThumbnail(safeTrim(updatedBook.thumbnail()));
        if (updatedBook.language() != null)
            book.setLanguage(safeTrim(updatedBook.language()));
        if (updatedBook.isbn() != null)
            book.setIsbn(updatedBook.isbn().trim());
        if (updatedBook.rating() != null)
            book.setRating(updatedBook.rating());
        if (updatedBook.publishedYear() != null)
            book.setPublishedYear(updatedBook.publishedYear());
        if (updatedBook.didacticTag() != null)
            book.setDidacticTag(updatedBook.didacticTag());
        if (updatedBook.readingLevel() != null)
            book.setReadingLevel(safeTrim(updatedBook.readingLevel()));
        if (updatedBook.ageRange() != null)
            book.setAgeRange(safeTrim(updatedBook.ageRange()));

        if (updatedBook.inventories() != null) {
            book.getInventories().clear();
            bookRepository.saveAndFlush(book);
            replaceInventoriesFromDto(book, updatedBook.inventories());
        }

        recomputeBookCopyTotals(book);

        BookEntity savedBook = bookRepository.save(book);
        savedBook.getInventories().forEach(this::reconcileCopiesForInventory);
        return toDTO(savedBook);
    }

    public BookDTO addManualBook(CreateBookRequestDTO request, String smartschoolUid) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Titel is verplicht");
        }

        BookEntity book = new BookEntity();
        book.setTitle(request.title().trim());
        book.setAuthors(cleanStringList(request.authors()));
        book.setPublisher(safeTrim(request.publisher()));
        book.setDescription(safeTrim(request.description()));
        book.setPageCount(request.pageCount() != null ? request.pageCount() : 0);
        book.setCategories(cleanStringList(request.categories()));
        book.setThumbnail(safeTrim(request.thumbnail()));
        book.setLanguage(safeTrim(request.language()));
        book.setRating(request.rating() != null ? request.rating() : 0.0);
        book.setPublishedYear(request.publishedYear());
        book.setSpotlight(Boolean.TRUE.equals(request.spotlight()));
        book.setDidacticTag(request.didacticTag());
        book.setLabels(cleanStringList(request.labels()));
        book.setReadingLevel(safeTrim(request.readingLevel()));
        book.setAgeRange(safeTrim(request.ageRange()));
        book.setIsbn("NOISBN-" + java.util.UUID.randomUUID());

        if (request.inventories() != null && !request.inventories().isEmpty()) {
            replaceInventoriesFromRequest(book, request.inventories(), smartschoolUid);
        } else {
            int totalCopies = request.totalCopies() != null ? request.totalCopies() : 1;
            int availableCopies = request.availableCopies() != null ? request.availableCopies() : totalCopies;
            applySingleInventoryForCurrentUser(book, smartschoolUid, null, totalCopies, availableCopies);
        }

        recomputeBookCopyTotals(book);

        BookEntity saved = bookRepository.saveAndFlush(book);
        saved.getInventories().forEach(this::reconcileCopiesForInventory);

        BookEntity reloaded = bookRepository.findDetailedById(saved.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Boek werd opgeslagen maar kon niet opnieuw geladen worden"));

        return toDTO(reloaded);
    }

    @Transactional(readOnly = true)
    public List<SnowballSectionDTO> getSnowballSections(Long bookId, UserRoles callerRole, String currentUserUid) {
        Pageable top12 = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "rating"));
        BookEntity book = bookRepository.findDetailedById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        List<SnowballSectionDTO> sections = new ArrayList<>();

        if (!book.getAuthors().isEmpty()) {
            String mainAuthor = book.getAuthors().get(0);
            List<BookDTO> authorBooks = bookRepository
                    .findByAuthorsInAndIdNot(book.getAuthors(), bookId, top12)
                    .stream()
                    .filter(b -> canSeeDidactic(callerRole) || !b.isDidacticTag())
                    .map(b -> toVisibleBookDTO(b, callerRole, currentUserUid))
                    .filter(Objects::nonNull)
                    .toList();

            if (!authorBooks.isEmpty()) {
                sections.add(new SnowballSectionDTO(
                        "AUTHOR", mainAuthor, authorBooks));
            }
        }

        // categorieeen
        if (!book.getCategories().isEmpty()) {
            for (String category : book.getCategories()) {
                List<BookDTO> categoryBooks = bookRepository
                        .findByCategoriesInAndIdNot(List.of(category), bookId, top12)
                        .stream()
                        .filter(b -> canSeeDidactic(callerRole) || !b.isDidacticTag())
                        .map(b -> toVisibleBookDTO(b, callerRole, currentUserUid))
                        .filter(Objects::nonNull)
                        .toList();

                if (!categoryBooks.isEmpty()) {
                    sections.add(new SnowballSectionDTO("CATEGORY", category, categoryBooks));
                }
            }
        }
        return sections;
    }

    public BookDTO getBookByBarcode(String barcode, UserRoles callerRole, String currentUserUid) {
        Optional<BookCopyEntity> copy = bookCopyRepository.findByBarcode(barcode);
        if (copy.isPresent()) {
            BookEntity book = bookRepository.findDetailedById(copy.get().getInventory().getBook().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boek niet gevonden"));
            BookDTO dto = toVisibleBookDTO(book, callerRole, currentUserUid);
            if (dto == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Boek niet gevonden");
            }
            return dto;
        }

        // Fall back naar ISBN als het barcode niet is gevonden
        BookEntity bookByIsbn = bookRepository.findByIsbn(barcode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barcode niet gevonden"));
        BookDTO dto = toVisibleBookDTO(bookByIsbn, callerRole, currentUserUid);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Boek niet gevonden");
        }
        return dto;
    }

    public BookCopyLabelDTO getCopyByBarcode(Long bookId, String barcode) {
        BookCopyEntity copy = bookCopyRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barcode niet gevonden"));

        if (!Objects.equals(copy.getInventory().getBook().getId(), bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Barcode hoort niet bij dit boek");
        }

        return new BookCopyLabelDTO(
                copy.getId(),
                copy.getBarcode(),
                copy.getCopyNumber(),
                copy.getInventory().getBook().getTitle(),
                copy.getInventory().getBook().getIsbn(),
                normalizeCampus(copy.getInventory().getCampus()),
                copy.getCopyCondition() != null ? copy.getCopyCondition().name() : "GOOD");
    }

    public List<BookCopyLabelDTO> getCopyLabelsForBook(Long bookId, String currentUserUid) {
        Long schoolId = requireRequesterSchoolId(currentUserUid);
        BookEntity book = bookRepository.findDetailedById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        List<BookInventoryEntity> inventories = book.getInventories().stream()
                .filter(inv -> Objects.equals(inv.getSchool().getId(), schoolId))
                .toList();

        List<BookCopyEntity> allCopies = inventories.stream()
                .flatMap(inv -> bookCopyRepository.findByInventory(inv).stream())
                .toList();

        List<BookCopyEntity> needsBarcode = allCopies.stream()
                .filter(c -> c.getBarcode() == null)
                .toList();

        if (!needsBarcode.isEmpty()) {
            needsBarcode.forEach(c -> c.setBarcode(generateEan13(c.getId())));
            bookCopyRepository.saveAll(needsBarcode);
        }

        return inventories.stream()
                .flatMap(inv -> {
                    String campus = normalizeCampus(inv.getCampus());
                    return bookCopyRepository.findByInventory(inv).stream()
                            .map(c -> new BookCopyLabelDTO(
                                    c.getId(),
                                    c.getBarcode(),
                                    c.getCopyNumber(),
                                    book.getTitle(),
                                    book.getIsbn(),
                                    campus,
                                    c.getCopyCondition() != null ? c.getCopyCondition().name() : "GOOD"));
                })
                .toList();
    }

    public List<BookCopyLabelDTO> getCopyLabelsForInventory(Long bookId, Long inventoryId) {
        BookEntity book = bookRepository.findDetailedById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        BookInventoryEntity inventory = book.getInventories().stream()
                .filter(inv -> Objects.equals(inv.getId(), inventoryId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventaris niet gevonden"));

        List<BookCopyEntity> copies = bookCopyRepository.findByInventory(inventory);

        List<BookCopyEntity> needsBarcode = copies.stream()
                .filter(c -> c.getBarcode() == null)
                .toList();

        if (!needsBarcode.isEmpty()) {
            needsBarcode.forEach(c -> c.setBarcode(generateEan13(c.getId())));
            bookCopyRepository.saveAll(needsBarcode);
        }

        String campus = normalizeCampus(inventory.getCampus());

        return copies.stream()
                .map(c -> new BookCopyLabelDTO(
                        c.getId(),
                        c.getBarcode(),
                        c.getCopyNumber(),
                        book.getTitle(),
                        book.getIsbn(),
                        campus,
                        c.getCopyCondition() != null ? c.getCopyCondition().name() : "GOOD"))
                .toList();
    }

    public List<BookCopyLabelDTO> getCopyLabelsForSchool(String currentUserUid) {
        Long schoolId = requireRequesterSchoolId(currentUserUid);
        List<BookInventoryEntity> inventories = bookInventoryRepository.findBySchool_Id(schoolId);

        List<BookCopyEntity> needsBarcode = inventories.stream()
                .flatMap(inv -> bookCopyRepository.findByInventory(inv).stream())
                .filter(c -> c.getBarcode() == null)
                .toList();

        if (!needsBarcode.isEmpty()) {
            needsBarcode.forEach(c -> c.setBarcode(generateEan13(c.getId())));
            bookCopyRepository.saveAll(needsBarcode);
        }

        return inventories.stream()
                .flatMap(inv -> {
                    String campus = normalizeCampus(inv.getCampus());
                    String bookTitle = inv.getBook().getTitle();
                    String isbn = inv.getBook().getIsbn();
                    return bookCopyRepository.findByInventory(inv).stream()
                            .map(c -> new BookCopyLabelDTO(
                                    c.getId(),
                                    c.getBarcode(),
                                    c.getCopyNumber(),
                                    bookTitle,
                                    isbn,
                                    campus,
                                    c.getCopyCondition() != null ? c.getCopyCondition().name() : "GOOD"));
                })
                .toList();
    }

    public void updateCopyCondition(Long copyId, BookCopyCondition newCondition, String notes) {
        BookCopyEntity copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exemplaar niet gevonden."));

        BookCopyCondition previousCondition = copy.getCopyCondition();
        copy.setCopyCondition(newCondition);
        if (notes != null) copy.setNotes(notes);
        bookCopyRepository.save(copy);

        if (previousCondition != newCondition) {
            inventoryAdjustmentService.adjustForConditionChange(copy.getInventory(), previousCondition, newCondition);
        }
    }

    // region Helper functies
    private static String generateEan13(long copyId) {
        // prefix 200-299 is gereserveerd voor intern gebruik (we moeten niet aan registratie aanmaken bij GS1 als we hiertussen blijven)
        String raw = String.format("200%09d", copyId);

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = raw.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;

        return raw + checkDigit;
    }

    private BookDTO toDTO(BookEntity book) {
        List<String> authors = book.getAuthors() == null
                ? new ArrayList<>()
                : new ArrayList<>(book.getAuthors());

        List<String> categories = book.getCategories() == null
                ? new ArrayList<>()
                : new ArrayList<>(book.getCategories());

        List<String> labels = book.getLabels() == null
                ? new ArrayList<>()
                : new ArrayList<>(book.getLabels());

        List<BookInventoryDTO> inventories = book.getInventories() == null
                ? new ArrayList<>()
                : book.getInventories().stream()
                        .sorted(Comparator
                                .comparing(
                                        (BookInventoryEntity inventory) -> inventory.getSchool().getName(),
                                        Comparator.nullsLast(String::compareToIgnoreCase))
                                .thenComparing(
                                        BookInventoryEntity::getCampus,
                                        Comparator.nullsLast(String::compareToIgnoreCase)))
                        .map(this::toInventoryDTO)
                        .toList();

        return new BookDTO(
                book.getId(),
                book.getTitle(),
                authors,
                book.getPublisher(),
                book.getDescription(),
                book.getPageCount(),
                categories,
                book.getThumbnail(),
                book.getLanguage(),
                book.getRating(),
                book.getIsbn(),
                book.getPublishedYear(),
                book.isDidacticTag(),
                labels,
                book.getReadingLevel(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.getAgeRange(),
                book.getPreviewLink(),
                inventories);
    }

    /**
     * Zorgt dat het aantal exemplaren in de database overeenkomt met het aantal dat in de inventaris staat. 
     * Als er te weinig exemplaren zijn, worden er nieuwe exemplaren aangemaakt. 
     * Als er te veel exemplaren zijn, worden er exemplaren verwijderd, waarbij eerst exemplaren in slechte staat worden verwijderd.
     * @param inventory
     */
    public void reconcileCopiesForInventory(BookInventoryEntity inventory) {
        List<BookCopyEntity> allCopies = bookCopyRepository.findByInventory(inventory);
        List<BookCopyEntity> nonLostCopies = allCopies.stream()
                .filter(c -> c.getCopyCondition() != BookCopyCondition.LOST)
                .toList();

        int target = inventory.getTotalCopies() == null ? 0 : inventory.getTotalCopies();
        int current = nonLostCopies.size();

        if (current < target) {
            int maxCopyNumber = allCopies.stream()
                    .mapToInt(BookCopyEntity::getCopyNumber)
                    .max().orElse(0);
            List<BookCopyEntity> toCreate = new ArrayList<>();
            for (int i = 1; i <= (target - current); i++) {
                BookCopyEntity copy = new BookCopyEntity();
                copy.setInventory(inventory);
                copy.setCopyCondition(BookCopyCondition.GOOD);
                copy.setCopyNumber(maxCopyNumber + i);
                toCreate.add(copy);
            }
            bookCopyRepository.saveAll(toCreate);
        } else if (current > target) {
            List<BookCopyEntity> candidates = new ArrayList<>();
            nonLostCopies.stream()
                    .filter(c -> c.getCopyCondition() == BookCopyCondition.BROKEN)
                    .forEach(candidates::add);
            nonLostCopies.stream()
                    .filter(c -> c.getCopyCondition() == BookCopyCondition.DAMAGED)
                    .forEach(candidates::add);
            nonLostCopies.stream()
                    .filter(c -> c.getCopyCondition() == BookCopyCondition.GOOD)
                    .forEach(candidates::add);
            int toRemove = current - target;
            bookCopyRepository.deleteAll(candidates.subList(0, Math.min(toRemove, candidates.size())));
        }
    }

    private BookInventoryDTO toInventoryDTO(BookInventoryEntity inventory) {
        int damaged = (int) bookCopyRepository.countByInventoryAndCopyCondition(inventory, BookCopyCondition.DAMAGED);
        int broken = (int) bookCopyRepository.countByInventoryAndCopyCondition(inventory, BookCopyCondition.BROKEN);
        int lost = (int) bookCopyRepository.countByInventoryAndCopyCondition(inventory, BookCopyCondition.LOST);

        return new BookInventoryDTO(
                inventory.getId(),
                inventory.getSchool().getId(),
                inventory.getSchool().getName(),
                normalizeCampus(inventory.getCampus()),
                inventory.getTotalCopies(),
                inventory.getAvailableCopies(),
                damaged,
                broken,
                lost);
    }

    private void replaceInventoriesFromRequest(BookEntity book,
            List<CreateBookInventoryRequestDTO> requestInventories,
            String smartschoolUid) {
        book.getInventories().clear();

        for (CreateBookInventoryRequestDTO requestInventory : requestInventories) {
            if (requestInventory == null) {
                continue;
            }

            SchoolEntity school = requestInventory.schoolId() != null
                    ? resolveSchoolById(requestInventory.schoolId())
                    : resolveSchoolForUser(smartschoolUid);

            String campus = normalizeCampus(requestInventory.campus());
            int totalCopies = requestInventory.totalCopies() != null ? requestInventory.totalCopies() : 0;
            int availableCopies = requestInventory.availableCopies() != null
                    ? requestInventory.availableCopies()
                    : totalCopies;

            validateInventoryCounts(totalCopies, availableCopies);
            addInventory(book, school, campus, totalCopies, availableCopies);
        }
    }

    private void replaceInventoriesFromDto(BookEntity book, List<BookInventoryDTO> inventoryDTOs) {
        for (BookInventoryDTO inventoryDTO : inventoryDTOs) {
            if (inventoryDTO == null) {
                continue;
            }

            if (inventoryDTO.schoolId() == null) {
                throw new IllegalArgumentException("Elke inventarisregel moet een schoolId hebben");
            }

            int totalCopies = inventoryDTO.totalCopies() != null ? inventoryDTO.totalCopies() : 0;
            int availableCopies = inventoryDTO.availableCopies() != null ? inventoryDTO.availableCopies() : totalCopies;

            validateInventoryCounts(totalCopies, availableCopies);
            addInventory(
                    book,
                    resolveSchoolById(inventoryDTO.schoolId()),
                    normalizeCampus(inventoryDTO.campus()),
                    totalCopies,
                    availableCopies);
        }
    }

    private void applySingleInventoryForCurrentUser(BookEntity book,
            String smartschoolUid,
            String campus,
            int totalCopies,
            int availableCopies) {
        validateInventoryCounts(totalCopies, availableCopies);
        book.getInventories().clear();
        addInventory(book, resolveSchoolForUser(smartschoolUid), normalizeCampus(campus), totalCopies, availableCopies);
    }

    private void addInventory(BookEntity book,
            SchoolEntity school,
            String campus,
            int totalCopies,
            int availableCopies) {
        BookInventoryEntity inventory = new BookInventoryEntity();
        inventory.setBook(book);
        inventory.setSchool(school);
        inventory.setCampus(campus);
        inventory.setTotalCopies(totalCopies);
        inventory.setAvailableCopies(availableCopies);
        book.getInventories().add(inventory);
    }

    private void addOrIncreaseInventory(
            BookEntity book,
            SchoolEntity school,
            String campus,
            int totalCopiesToAdd,
            int availableCopiesToAdd) {

        BookInventoryEntity existingInventory = findInventory(book, school, campus);

        if (existingInventory == null) {
            addInventory(book, school, campus, totalCopiesToAdd, availableCopiesToAdd);
        } else {
            existingInventory.setTotalCopies(
                    safeCopyCount(existingInventory.getTotalCopies()) + totalCopiesToAdd);

            existingInventory.setAvailableCopies(
                    safeCopyCount(existingInventory.getAvailableCopies()) + availableCopiesToAdd);
        }

        recomputeBookCopyTotals(book);
        BookEntity saved = bookRepository.save(book);
        saved.getInventories().forEach(this::reconcileCopiesForInventory);
    }

    private BookInventoryEntity findInventory(BookEntity book, SchoolEntity school, String campus) {
        String normalizedCampus = normalizeCampus(campus);
        Long schoolId = school.getId();

        if (book.getInventories() == null) {
            return null;
        }

        return book.getInventories().stream()
                .filter(inventory -> inventory.getSchool() != null)
                .filter(inventory -> Objects.equals(inventory.getSchool().getId(), schoolId))
                .filter(inventory -> Objects.equals(normalizeCampus(inventory.getCampus()), normalizedCampus))
                .findFirst()
                .orElse(null);
    }

    private int safeCopyCount(Integer value) {
        return value == null ? 0 : value;
    }

    private BookEntity findDuplicateBookForNoIsbnImport(
            String title,
            List<String> authors,
            String publisher,
            SchoolEntity school) {

        List<String> normalizedAuthors = normalizeDuplicateList(authors);

        if (normalizedAuthors.isEmpty()) {
            return null;
        }

        String normalizedTitle = normalizeDuplicateText(title);
        String normalizedPublisher = normalizeDuplicateText(publisher);

        return bookRepository.findPossibleDuplicateBooksWithoutIsbn(
                title.trim(),
                publisher,
                school.getId())
                .stream()
                .filter(book -> Objects.equals(normalizeDuplicateText(book.getTitle()), normalizedTitle))
                .filter(book -> Objects.equals(normalizeDuplicateText(book.getPublisher()), normalizedPublisher))
                .filter(book -> Objects.equals(normalizeDuplicateList(book.getAuthors()), normalizedAuthors))
                .findFirst()
                .orElse(null);
    }

    private List<String> normalizeDuplicateList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .map(this::normalizeDuplicateText)
                .filter(value -> !value.isBlank())
                .sorted()
                .toList();
    }

    private String normalizeDuplicateText(String value) {
        if (value == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private void recomputeBookCopyTotals(BookEntity book) {
        int totalCopies = book.getInventories() == null
                ? 0
                : book.getInventories().stream()
                        .map(BookInventoryEntity::getTotalCopies)
                        .filter(value -> value != null)
                        .mapToInt(Integer::intValue)
                        .sum();

        int availableCopies = book.getInventories() == null
                ? 0
                : book.getInventories().stream()
                        .map(BookInventoryEntity::getAvailableCopies)
                        .filter(value -> value != null)
                        .mapToInt(Integer::intValue)
                        .sum();

        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
    }

    private void applyBulkIsbnImportFields(
            BookEntity book,
            String categories,
            String labels,
            String readingLevel) {

        if (categories != null && !categories.isBlank()) {
            book.setCategories(splitImportList(categories));
        } else if (book.getCategories() == null) {
            book.setCategories(new ArrayList<>());
        }

        book.setLabels(splitImportList(labels));
        book.setReadingLevel(normalizeReadingLevel(readingLevel));
    }

    private void validateInventoryCounts(int totalCopies, int availableCopies) {
        if (totalCopies < 0) {
            throw new IllegalArgumentException("Totaal aantal exemplaren mag niet negatief zijn");
        }
        if (availableCopies < 0) {
            throw new IllegalArgumentException("Beschikbare exemplaren mogen niet negatief zijn");
        }
        if (availableCopies > totalCopies) {
            throw new IllegalArgumentException(
                    "Beschikbare exemplaren mogen niet groter zijn dan totaal aantal exemplaren");
        }
    }

    private BookEntity buildBookEntityFromGoogle(String isbn) {
        String url = UriComponentsBuilder
                .fromUriString(googleBooksApiUrl)
                .queryParam("q", "isbn:" + isbn)
                .queryParam("key", googleBooksApiKey)
                .toUriString();

        log.info("Zoeken naar ISBN: {} met key suffix: {}", isbn,
                googleBooksApiKey.length() >= 4
                        ? googleBooksApiKey.substring(googleBooksApiKey.length() - 4)
                        : "too-short");

        GoogleBooksResponse response = null;

        try {
            // De eerste API Call (met fout-afvanging)
            response = restTemplate.getForObject(url, GoogleBooksResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Dit vangt fouten zoals 429 (Too Many Requests) of 403 (Quota Exceeded) netjes af
            log.error("Google API weigerde het verzoek! Status: {}, Reden: {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new RuntimeException("De Google API weigert het verzoek tijdelijk (Status " + e.getStatusCode()
                    + "). Wacht even en probeer het opnieuw.");
        } catch (Exception e) {
            log.error("Onverwachte fout bij ophalen ISBN: {}", e.getMessage(), e);
            throw new RuntimeException("Er ging iets mis bij het communiceren met Google Books.");
        }

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalArgumentException("Geen boek gevonden voor ISBN: " + isbn);
        }

        VolumeInfo volumeInfo = response.getItems().get(0).getVolumeInfo();

        BookEntity book = new BookEntity();
        book.setTitle(volumeInfo.getTitle() != null ? volumeInfo.getTitle() : "Onbekende Titel");
        book.setAuthors(volumeInfo.getAuthors() != null ? volumeInfo.getAuthors() : new java.util.ArrayList<>());
        book.setPublisher(volumeInfo.getPublisher());
        book.setDescription(volumeInfo.getDescription());
        book.setPageCount(volumeInfo.getPageCount() != null ? volumeInfo.getPageCount() : 0);
        book.setCategories(
                volumeInfo.getCategories() != null ? volumeInfo.getCategories() : new java.util.ArrayList<>());

        if (volumeInfo.getImageLinks() != null) {
            book.setThumbnail(volumeInfo.getImageLinks().getThumbnail());
        } else {
            book.setThumbnail("");
        }

        book.setLanguage(volumeInfo.getLanguage());
        book.setRating(volumeInfo.getAverageRating() != null ? volumeInfo.getAverageRating() : 0.0);
        book.setIsbn(isbn);
        book.setPublishedYear(extractYear(volumeInfo.getPublishedDate()));
        book.setSpotlight(false);
        book.setInventories(new ArrayList<>());
        book.setTotalCopies(0);
        book.setAvailableCopies(0);

        String finalReaderLink = null;

        try {
            String originalTitle = book.getTitle();
            String originalTitleLower = originalTitle.toLowerCase().trim();
            String authorQuery = (book.getAuthors() != null && !book.getAuthors().isEmpty()) ? book.getAuthors().get(0)
                    : "";
            String searchQuery = (originalTitle + " " + authorQuery).trim();

            java.net.URI searchUri = UriComponentsBuilder
                    .fromUriString(googleBooksApiUrl)
                    .queryParam("q", searchQuery)
                    .queryParam("key", googleBooksApiKey)
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> searchResponse = restTemplate.getForObject(searchUri, java.util.Map.class);

            if (searchResponse != null && searchResponse.get("items") instanceof java.util.List) {
                java.util.List<?> items = (java.util.List<?>) searchResponse.get("items");

                for (Object itemObj : items) {
                    if (itemObj instanceof java.util.Map) {
                        java.util.Map<?, ?> item = (java.util.Map<?, ?>) itemObj;

                        // 1. Controleer of de titel van dit zoekresultaat wel overeenkomt met ons boek!
                        boolean isTitleMatch = false;
                        if (item.get("volumeInfo") instanceof java.util.Map) {
                            java.util.Map<?, ?> itemVolumeInfo = (java.util.Map<?, ?>) item.get("volumeInfo");
                            Object itemTitleObj = itemVolumeInfo.get("title");

                            if (itemTitleObj instanceof String) {
                                String itemTitleLower = ((String) itemTitleObj).toLowerCase().trim();

                                // We checken of de titels sterk overeenkomen
                                // (Gelijk aan elkaar, of de ene is een onderdeel van de andere i.v.m.
                                // ondertitels)
                                if (itemTitleLower.equals(originalTitleLower) ||
                                        itemTitleLower.startsWith(originalTitleLower) ||
                                        originalTitleLower.startsWith(itemTitleLower)) {
                                    isTitleMatch = true;
                                }
                            }
                        }

                        // 2. Als de titel klopt, dán pas kijken we naar de lees-link
                        if (isTitleMatch && item.get("accessInfo") instanceof java.util.Map) {
                            java.util.Map<?, ?> accessInfo = (java.util.Map<?, ?>) item.get("accessInfo");

                            Object viewabilityObj = accessInfo.get("viewability");
                            Object webReaderLinkObj = accessInfo.get("webReaderLink");

                            if (viewabilityObj instanceof String && webReaderLinkObj instanceof String) {
                                String viewability = (String) viewabilityObj;
                                String webReaderLink = (String) webReaderLinkObj;

                                if (!"NO_PAGES".equals(viewability)
                                        && webReaderLink.contains("play.google.com/books/reader")) {
                                    finalReaderLink = webReaderLink.replace("http://", "https://");
                                    break; // Perfecte match gevonden, stop met zoeken!
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fout bij ophalen e-book editie op de achtergrond: {}", e.getMessage(), e);
        }

        book.setPreviewLink(finalReaderLink);

        return book;
    }

    private boolean titlesMatch(String excelTitle, String fetchedTitle) {
        return normalizeTitle(excelTitle).equals(normalizeTitle(fetchedTitle));
    }

    private String normalizeTitle(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private List<String> cleanStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList());
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeCampus(String campus) {
        String trimmed = safeTrim(campus);
        return trimmed == null ? "" : trimmed;
    }

    private SchoolEntity resolveSchoolForUser(String smartschoolUid) {
        if (smartschoolUid == null || smartschoolUid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Je moet ingelogd zijn om een boek toe te voegen");
        }

        return userRepository.findBySmartschoolUid(smartschoolUid)
                .map(UserEntity::getSchool)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Geen school gevonden voor de ingelogde gebruiker"));
    }

    private SchoolEntity resolveSchoolById(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "School niet gevonden voor id: " + schoolId));
    }

    private String determineAgeRangeFromStudentClass(UserEntity user) {
        if (user.getClasses() == null || user.getClasses().isEmpty()) {
            throw new IllegalArgumentException("Student heeft geen klas");
        }

        SchoolClassEntity schoolClass = user.getClasses().iterator().next();

        if (schoolClass.getName() == null || schoolClass.getName().isBlank()) {
            throw new IllegalArgumentException("Klasnaam ontbreekt");
        }

        char firstChar = schoolClass.getName().trim().charAt(0);

        return switch (firstChar) {
            case '1', '2' -> "Eerste graad";
            case '3', '4' -> "Tweede graad";
            case '5', '6', '7' -> "Derde graad";
            default -> throw new IllegalArgumentException("Onbekende klasnaam: " + schoolClass.getName());
        };
    }

    private boolean canSeeDidactic(UserRoles role) {
        return role == UserRoles.TEACHER
                || role == UserRoles.LIBRARIAN
                || role == UserRoles.ADMIN;
    }

    private boolean restrictToOwnSchool(UserRoles role) {
        return role == UserRoles.STUDENT;
    }

    private Long getRequesterSchoolId(String currentUserUid) {
        return userRepository.findDetailedBySmartschoolUid(currentUserUid)
                .map(UserEntity::getSchool)
                .map(SchoolEntity::getId)
                .orElse(null);
    }

    private List<BookInventoryEntity> getVisibleInventories(
            BookEntity book,
            UserRoles role,
            String currentUserUid) {
        if (!restrictToOwnSchool(role)) {
            return book.getInventories() == null ? List.of() : book.getInventories();
        }

        Long requesterSchoolId = getRequesterSchoolId(currentUserUid);
        if (requesterSchoolId == null || book.getInventories() == null) {
            return List.of();
        }

        return book.getInventories().stream()
                .filter(inventory -> inventory.getSchool() != null &&
                        Objects.equals(inventory.getSchool().getId(), requesterSchoolId))
                .toList();
    }

    private BookDTO toVisibleBookDTO(
            BookEntity book,
            UserRoles role,
            String currentUserUid) {

        List<BookInventoryEntity> visibleInventories = getVisibleInventories(book, role, currentUserUid);

        if (restrictToOwnSchool(role) && visibleInventories.isEmpty()) {
            return null;
        }

        int totalCopies = visibleInventories.stream()
                .map(BookInventoryEntity::getTotalCopies)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int availableCopies = visibleInventories.stream()
                .map(BookInventoryEntity::getAvailableCopies)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        List<BookInventoryDTO> inventoryDTOs = visibleInventories.stream()
                .sorted(Comparator
                        .comparing(
                                (BookInventoryEntity inventory) -> inventory.getSchool().getName(),
                                Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(
                                BookInventoryEntity::getCampus,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toInventoryDTO)
                .toList();

        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthors() == null ? new ArrayList<>() : new ArrayList<>(book.getAuthors()),
                book.getPublisher(),
                book.getDescription(),
                book.getPageCount(),
                book.getCategories() == null ? new ArrayList<>() : new ArrayList<>(book.getCategories()),
                book.getThumbnail(),
                book.getLanguage(),
                book.getRating(),
                book.getIsbn(),
                book.getPublishedYear(),
                book.isDidacticTag(),
                book.getLabels() == null ? new ArrayList<>() : new ArrayList<>(book.getLabels()),
                book.getReadingLevel(),
                totalCopies,
                availableCopies,
                book.getAgeRange(),
                book.getPreviewLink(),
                inventoryDTOs);
    }

    private Long requireRequesterSchoolId(String currentUserUid) {
        Long schoolId = getRequesterSchoolId(currentUserUid);
        if (schoolId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Geen school gevonden voor de ingelogde gebruiker");
        }
        return schoolId;
    }
    private Sort resolveSort(String sortBy) {
        if ("title_asc".equals(sortBy)) return Sort.by("title").ascending();
        if ("newest".equals(sortBy)) return Sort.by("id").descending();
        return Sort.unsorted();
    }

    private Map<String, Integer> getImportColumnIndexes(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Het Excelbestand heeft geen header rij");
        }

        Map<String, Integer> columns = new HashMap<>();

        for (Cell cell : headerRow) {
            String key = normalizeImportHeader(formatter.formatCellValue(cell));

            if (!key.isBlank()) {
                columns.put(key, cell.getColumnIndex());
            }
        }

        return columns;
    }

    private String normalizeImportHeader(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("'", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String getImportCell(
            Row row,
            Map<String, Integer> columns,
            DataFormatter formatter,
            String columnName) {

        Integer columnIndex = columns.get(normalizeImportHeader(columnName));

        if (columnIndex == null) {
            return "";
        }

        return formatter.formatCellValue(row.getCell(columnIndex)).trim();
    }

    private boolean rowIsBlank(Row row, DataFormatter formatter) {
        short firstCell = row.getFirstCellNum();
        short lastCell = row.getLastCellNum();

        if (firstCell < 0 || lastCell < 0) {
            return true;
        }

        for (int i = firstCell; i < lastCell; i++) {
            if (!formatter.formatCellValue(row.getCell(i)).trim().isBlank()) {
                return false;
            }
        }

        return true;
    }

    private List<String> splitImportList(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                List.of(value.split(";"))
                        .stream()
                        .map(String::trim)
                        .filter(item -> !item.isBlank())
                        .toList());
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "Geen taal ingegeven";
        }

        String trimmedLanguage = language.trim().toLowerCase(Locale.ROOT);

        return switch (trimmedLanguage) {
            case "nl", "ne", "nederlands", "dutch" -> "nl";
            case "en", "engels", "english" -> "en";
            case "fr", "frans", "french" -> "fr";
            default -> trimmedLanguage;
        };
    }

    private String normalizeReadingLevel(String readingLevel) {
        if (readingLevel == null || readingLevel.isBlank()) {
            return "A";
        }

        String normalized = readingLevel.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "A", "B", "C", "D" -> normalized;
            default -> "A";
        };
    }

    private boolean parseDidacticBoolean(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "ja", "yes", "true", "1", "waar" -> true;
            default -> false;
        };
    }

    private Integer parsePublishedYearOrNull(String value) {
        Integer parsed = parseIntegerOrNull(value);

        if (parsed == null || parsed <= 0) {
            return null;
        }

        if (parsed > Year.now().getValue()) {
            throw new IllegalArgumentException("Jaar van uitgave mag niet in de toekomst liggen");
        }

        return parsed;
    }

    private int parsePositiveIntOrDefault(String value, int fallback, String fieldName) {
        Integer parsed = parseIntegerOrNull(value);

        if (parsed == null || parsed <= 0) {
            return fallback;
        }

        return parsed;
    }

    private int parseNonNegativeIntOrDefault(String value, int fallback, String fieldName) {
        Integer parsed = parseIntegerOrNull(value);

        if (parsed == null) {
            return fallback;
        }

        if (parsed < 0) {
            throw new IllegalArgumentException(fieldName + " mag niet negatief zijn");
        }

        return parsed;
    }

    private Integer parseIntegerOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            String normalized = value.trim().replace(",", ".");
            return (int) Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + value + "' is geen geldig getal");
        }
    }

    // endregion
}