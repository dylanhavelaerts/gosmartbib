package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.BookDTO;
import edu.ap.gosmartlib.dto.BookFilterRequest;
import edu.ap.gosmartlib.dto.BookInventoryDTO;
import edu.ap.gosmartlib.dto.CreateBookInventoryRequestDTO;
import edu.ap.gosmartlib.dto.CreateBookRequestDTO;
import edu.ap.gosmartlib.dto.googlebooks.GoogleBooksResponse;
import edu.ap.gosmartlib.dto.googlebooks.VolumeInfo;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.BookInventoryEntity;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.exceptions.NegativeValueException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.dto.importdto.ImportMismatchDTO;
import org.apache.poi.ss.usermodel.DataFormatter;
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

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final BookFilterValidator bookFilterValidator;
    private final SchoolRepository schoolRepository;

    @Value("${google.books.api.url}")
    private String googleBooksApiUrl;

    @Value("${google.books.api.key}")
    private String googleBooksApiKey;

    // Size = aantal items per pagina, page = welke pagina (0-based)
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

    public BookDTO addBookByIsbn(String isbn, String smartschoolUid, String campus, Integer amount) {
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("ISBN mag niet leeg zijn");
        }

        int copies = amount != null ? amount : 1;

        if (copies < 1) {
            throw new IllegalArgumentException("Aantal boeken moet minstens 1 zijn");
        }

        String normalizedIsbn = normalizeIsbn(isbn);
        SchoolEntity school = resolveSchoolForUser(smartschoolUid);
        String normalizedCampus = normalizeCampus(campus);

        BookEntity existingBook = bookRepository.findByIsbn(normalizedIsbn).orElse(null);

        if (existingBook != null) {
            if (hasInventoryForSchoolAndCampus(existingBook, school, normalizedCampus)) {
                throw new IllegalArgumentException("Boek bestaat al voor deze campus");
            }
            addInventory(existingBook, school, normalizedCampus, copies, copies);
            recomputeBookCopyTotals(existingBook);

            return toDTO(bookRepository.save(existingBook));
        }

        BookEntity newBook = buildBookEntityFromGoogle(normalizedIsbn);
        addInventory(newBook, school, normalizedCampus, copies, copies);
        recomputeBookCopyTotals(newBook);
        BookEntity savedBook = bookRepository.save(newBook);
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

    /**
     * Zoekt een boek op via het id in de database.
     * Als het gevonden wordt, wordt het omgezet naar een DTO en teruggegeven.
     * Als het niet gevonden wordt, gooit het een BookNotFoundException met het id.
     */
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

    /**
     * Zoekt een boek op via het id.
     * Als het niet gevonden wordt, gooit het een BookNotFoundException.
     * Als het gevonden wordt, wordt het verwijderd uit de database.
     */
    public void deleteBook(Long id) throws BookNotFoundException {
        BookEntity book = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
        bookRepository.delete(book);
    }

    public void updateSpotlight(Long id, boolean spotlight) {
        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        book.setSpotlight(spotlight);
        bookRepository.save(book);
    }

    /**
     * Zoekt boeken op basis van een zoekterm. Er wordt gezocht in zowel de titel
     * als de auteurs van het boek.
     * 
     * @param query De zoekterm om op te filteren. Als deze leeg is, worden alle
     *              boeken teruggegeven.
     * @return Een lijst van boeken die overeenkomen met de zoekterm, omgezet naar
     *         DTO's.
     */
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

            return bookRepository.searchByTitleOrAuthorOrCategoryForSchool(
                    query.trim(),
                    includeDidactic,
                    schoolId,
                    pageable)
                    .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid));
        }

        if (query == null || query.isBlank()) {
            return bookRepository.findAllFiltered(canSeeDidactic(callerRole), pageable).map(this::toDTO);
        }

        return bookRepository.searchByTitleOrAuthorOrCategory(query.trim(), includeDidactic, pageable)
                .map(this::toDTO);
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

    public Page<BookDTO> filterBooks(BookFilterRequest request, UserRoles callerRole, String currentUserUid) {
        bookFilterValidator.validate(request);

        Pageable pageable = PageRequest.of(request.page(), request.size());
        boolean includeDidactic = canSeeDidactic(callerRole);
        if (restrictToOwnSchool(callerRole)) {
            Long schoolId = requireRequesterSchoolId(currentUserUid);

            return bookRepository.filterBooksForSchool(
                    includeDidactic,
                    request.query(),
                    request.language(),
                    request.categories(),
                    request.labels(),
                    request.minPageCount(),
                    request.maxPageCount(),
                    request.minPubYear(),
                    request.maxPubYear(),
                    request.minRating(),
                    request.maxRating(),
                    schoolId,
                    pageable)
                    .map(book -> toVisibleBookDTO(book, callerRole, currentUserUid));
        }
        return bookRepository
                .filterBooks(
                        includeDidactic,
                        request.query(),
                        request.language(),
                        request.categories(),
                        request.labels(),
                        Boolean.TRUE.equals(request.didacticOnly()), // null-safe unbox moet erbij anders leerkrachtenpad geeft errors
                        request.minPageCount(),
                        request.maxPageCount(),
                        request.minPubYear(),
                        request.maxPubYear(),
                        request.minRating(),
                        request.maxRating(),
                        pageable)
                .map(this::toDTO);
    }

    public BulkImportResponseDTO importBooksFromExcel(MultipartFile file, String smartschoolUid,
            String campus) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload een excel file die niet leeg is");
        }

        List<ImportMismatchDTO> mismatches = new ArrayList<>();
        int totalRows = 0;
        int savedCount = 0;

        SchoolEntity importSchool = resolveSchoolForUser(smartschoolUid);
        String normalizedCampus = normalizeCampus(campus);

        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String isbn = normalizeIsbn(formatter.formatCellValue(row.getCell(0)));
                String excelTitle = formatter.formatCellValue(row.getCell(1)).trim();
                String amountText = formatter.formatCellValue(row.getCell(2)).trim();

                if (isbn.isBlank() && excelTitle.isBlank() && amountText.isBlank()) {
                    continue;
                }

                totalRows++;

                if (isbn.isBlank() || excelTitle.isBlank()) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            null,
                            "Geen ISBN nummer of titel"));
                    continue;
                }

                int amount;
                try {
                    amount = parseImportAmount(amountText);
                } catch (IllegalArgumentException e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            null,
                            e.getMessage()));
                    continue;
                }

                BookEntity existingBook = bookRepository.findByIsbn(isbn).orElse(null);

                if (existingBook != null) {

                    if (hasInventoryForSchoolAndCampus(existingBook, importSchool, normalizedCampus)) {
                        mismatches.add(new ImportMismatchDTO(
                                rowIndex + 1,
                                isbn,
                                excelTitle,
                                existingBook.getTitle(),
                                "Boek bestaat al voor deze campus",
                                amount));
                        continue;
                    }

                    if (!titlesMatch(excelTitle, existingBook.getTitle())) {
                        mismatches.add(new ImportMismatchDTO(
                                rowIndex + 1,
                                isbn,
                                excelTitle,
                                existingBook.getTitle(),
                                "De titel komt niet overeen (" + existingBook.getTitle() + ")",
                                amount));
                        continue;
                    }

                    try {
                        addInventory(existingBook, importSchool, normalizedCampus, amount, amount);
                        recomputeBookCopyTotals(existingBook);
                        bookRepository.save(existingBook);
                        savedCount++;
                    } catch (Exception e) {
                        mismatches.add(new ImportMismatchDTO(
                                rowIndex + 1,
                                isbn,
                                excelTitle,
                                existingBook.getTitle(),
                                "Onverwachte fout bij het toevoegen van de campusvoorraad", amount));
                    }

                    continue;
                }

                BookEntity newBook;

                try {
                    newBook = buildBookEntityFromGoogle(isbn);
                } catch (IllegalArgumentException e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            null,
                            "Geen boeken gevonden in Google Books",
                            amount));
                    continue;
                } catch (Exception e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            null,
                            "Onverwachte fout bij het ophalen uit Google Books", amount));
                    continue;
                }

                if (!titlesMatch(excelTitle, newBook.getTitle())) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            newBook.getTitle(),
                            "De titel komt niet overeen (" + newBook.getTitle() + ")", amount));
                    continue;
                }

                try {
                    addInventory(newBook, importSchool, normalizedCampus, amount, amount);
                    recomputeBookCopyTotals(newBook);
                    bookRepository.save(newBook);
                    savedCount++;
                } catch (Exception e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            newBook.getTitle(),
                            "Onverwachte fout bij het opslaan van het boek", amount));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Kon de excel file niet lezen", e);
        }

        return new BulkImportResponseDTO(
                totalRows,
                savedCount,
                mismatches.size(),
                mismatches);
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

        return toDTO(bookRepository.save(book));
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
        // Maakt random ISBN aan
        // Enorm kleine kans voor een dubbele ID
        // In dat geval, gewoon opnieuw indienen
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

        // Reload zo dat de DB-gegenereerde ISBN in de response DTO zit
        BookEntity reloaded = bookRepository.findDetailedById(saved.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Boek werd opgeslagen maar kon niet opnieuw geladen worden"));

        return toDTO(reloaded);
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
                inventories);
    }

    private BookInventoryDTO toInventoryDTO(BookInventoryEntity inventory) {
        return new BookInventoryDTO(
                inventory.getId(),
                inventory.getSchool().getId(),
                inventory.getSchool().getName(),
                normalizeCampus(inventory.getCampus()),
                inventory.getTotalCopies(),
                inventory.getAvailableCopies());
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

    // region Helper functies
    private BookEntity buildBookEntityFromGoogle(String isbn) {
        String url = UriComponentsBuilder
                .fromUriString(googleBooksApiUrl)
                .queryParam("q", "isbn:" + isbn)
                .queryParam("key", googleBooksApiKey)
                .toUriString();

        System.out.println("Using Google Books request with key suffix: " +
                (googleBooksApiKey.length() >= 4
                        ? googleBooksApiKey.substring(googleBooksApiKey.length() - 4)
                        : "too-short"));

        GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalArgumentException("Geen boek voor ISBN: " + isbn);
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
                || role == UserRoles.BIBLIOTHEEKBEHEERDER
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

    private int parseImportAmount(String amountText) {
        if (amountText == null || amountText.isBlank()) {
            throw new IllegalArgumentException("Aantal boeken is verplicht");
        }

        try {
            int amount = Integer.parseInt(amountText.trim());

            if (amount < 1) {
                throw new IllegalArgumentException("Aantal boeken moet minstens 1 zijn");
            }

            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Aantal boeken moet een geheel getal zijn");
        }
    }

    private boolean hasInventoryForSchoolAndCampus(BookEntity book, SchoolEntity school, String campus) {
        if (book.getInventories() == null || school == null || school.getId() == null) {
            return false;
        }

        String normalizedCampus = normalizeCampus(campus);

        return book.getInventories().stream()
                .anyMatch(inventory -> inventory.getSchool() != null
                        && Objects.equals(inventory.getSchool().getId(), school.getId())
                        && Objects.equals(normalizeCampus(inventory.getCampus()), normalizedCampus));
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return "";
        }

        return isbn
                .trim()
                .replace("-", "")
                .replace(" ", "");
    }
    // endregion
}
