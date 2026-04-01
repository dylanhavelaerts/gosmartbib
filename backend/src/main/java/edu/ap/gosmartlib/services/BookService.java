package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.BookDTO;
import edu.ap.gosmartlib.dto.CreateBookRequestDTO;
import edu.ap.gosmartlib.dto.googlebooks.GoogleBooksResponse;
import edu.ap.gosmartlib.dto.googlebooks.VolumeInfo;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.exceptions.NegativeValueException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;

import org.apache.catalina.User;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Value("${google.books.api.url}")
    private String googleBooksApiUrl;

    @Value("${google.books.api.key}")
    private String googleBooksApiKey;

    // Size = aantal items per pagina, page = welke pagina (0-based)
    public Page<BookDTO> getAllBooks(int page, int size, UserRoles callerRoles) {
        if (page < 0 || size <= 0)
            throw new NegativeValueException("Page number cannot be negative and size must be greater than 0");

        Pageable pageable = PageRequest.of(page, size);
        return bookRepository.findAllFiltered(canSeeDidactic(callerRoles), pageable)
                .map(this::toDTO);
    }

    public List<BookDTO> getAllBooksUnpaged(UserRoles  callerRoles) {
        boolean includeDidactic = canSeeDidactic(callerRoles);
        return bookRepository.findAll()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(this::toDTO)
                .toList();
    }

    public BookDTO searchBookByIsbn(String isbn) {
        BookEntity previewBook = buildBookEntityFromGoogle(isbn);
        return toDTO(previewBook);
    }

    public BookDTO addBookByIsbn(String isbn) {
        BookEntity newBook = buildBookEntityFromGoogle(isbn);
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
    public BookDTO getBookById(Long id, UserRoles callerRole) throws BookNotFoundException {

        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        if (book.isDidacticTag() && !canSeeDidactic(callerRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Je hebt geen toegang tot dit boek");
        }
        return toDTO(book);
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
     * DTO's.
     */
    public Page<BookDTO> searchByTitleOrAuthorOrCategory(String query, int page, int size, UserRoles callerRoles) {
        if (page < 0 || size <= 0)
            throw new NegativeValueException("Page number cannot be negative and size must be greater than 0");

        Pageable pageable = PageRequest.of(page, size);

        if (query == null || query.isBlank()) {
            return bookRepository.findAllFiltered(canSeeDidactic(callerRoles), pageable).map(this::toDTO);
        }

        return bookRepository.searchByTitleOrAuthorOrCategory(query.trim(), canSeeDidactic(callerRoles), pageable).map(this::toDTO);
    }

    public List<BookDTO> getTop4BooksInSpotlight(UserRoles callerRole) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        return bookRepository.findTop4BySpotlightTrueOrderByIdDesc()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(this::toDTO)
                .toList();
    }

    public List<BookDTO> getAllBooksInSpotlight(UserRoles callerRole) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        return bookRepository.findBySpotlightTrueOrderByIdDesc()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(this::toDTO)
                .toList();
    }

    public List<BookDTO> getLatestBooks(UserRoles callerRole) {
        boolean includeDidactic = canSeeDidactic(callerRole);
        return bookRepository.findTop4ByOrderByIdDesc()
                .stream()
                .filter(b -> includeDidactic || !b.isDidacticTag())
                .map(this::toDTO)
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
                .map(this::toDTO)
                .toList();
    }

    public Page<BookDTO> filterBooks(String language, List<String> categories, List<String> labels,
                                     Integer minPageCount,
                                     Integer maxPageCount, Integer minPubYear, Integer maxPubYear, int page, int size,UserRoles callerRoles) {
        if (page < 0 || size <= 0)
            throw new NegativeValueException("Page number cannot be negative and size must be greater than 0");

        if (minPageCount != null && maxPageCount != null && minPageCount > maxPageCount) {
            throw new IllegalArgumentException("minPageCount cannot be bigger than maxPageCount");
        }
        if (minPubYear != null && maxPubYear != null && minPubYear > maxPubYear) {
            throw new IllegalArgumentException("minPubYear cannot be bigger than maxPubYear");
        }

        Pageable pageable = PageRequest.of(page, size);
        return bookRepository
                .filterBooks(canSeeDidactic(callerRoles),language, categories, labels, minPageCount, maxPageCount, minPubYear, maxPubYear, pageable)
                .map(this::toDTO);
    }

    public BulkImportResponseDTO importBooksFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload een excel file die niet leeg is");
        }

        List<ImportMismatchDTO> mismatches = new ArrayList<>();
        int totalRows = 0;
        int savedCount = 0;

        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String isbn = formatter.formatCellValue(row.getCell(0)).trim();
                String excelTitle = formatter.formatCellValue(row.getCell(1)).trim();

                if (isbn.isBlank() && excelTitle.isBlank()) {
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

                if (bookRepository.existsByIsbn(isbn)) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            null,
                            "Boek bestaat al in de database"));
                    continue;
                }

                try {
                    BookEntity fetchedBook = buildBookEntityFromGoogle(isbn);

                    if (!titlesMatch(excelTitle, fetchedBook.getTitle())) {
                        mismatches.add(new ImportMismatchDTO(
                                rowIndex + 1,
                                isbn,
                                excelTitle,
                                fetchedBook.getTitle(),
                                "De titel komt niet overeen (" + fetchedBook.getTitle() + ")"));
                        continue;
                    }

                    bookRepository.save(fetchedBook);
                    savedCount++;

                } catch (IllegalArgumentException e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            null,
                            "Geen boeken gevonden in Google Books"));
                } catch (Exception e) {
                    mismatches.add(new ImportMismatchDTO(
                            rowIndex + 1,
                            isbn,
                            excelTitle,
                            null,
                            "Onverwachte fout bij het ophalen en opslaan"));
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
        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        if (updatedBook.title() != null && updatedBook.title().isBlank())
            throw new IllegalArgumentException("Titel mag niet leeg zijn");

        if (updatedBook.pageCount() != null && updatedBook.pageCount() < 0)
            throw new IllegalArgumentException("Paginacount mag niet negatief zijn");
        if (updatedBook.publishedYear() != null && updatedBook.publishedYear() <= 0)
            throw new IllegalArgumentException("Publicatiejaar moet groter zijn dan 0");

        if (updatedBook.publishedYear() != null && updatedBook.publishedYear() > Year.now().getValue())
            throw new IllegalArgumentException("Publicatiejaar mag niet in de toekomst liggen");
        if (updatedBook.totalCopies() != null && updatedBook.totalCopies() < 0)
            throw new IllegalArgumentException("Totaal aantal exemplaren mag niet negatief zijn");

        if (updatedBook.availableCopies() != null && updatedBook.availableCopies() < 0)
            throw new IllegalArgumentException("Beschikbare exemplaren mogen niet negatief zijn");

        int finalTotal = updatedBook.totalCopies() != null ? updatedBook.totalCopies() : book.getTotalCopies();
        int finalAvailable = updatedBook.availableCopies() != null ? updatedBook.availableCopies()
                : book.getAvailableCopies();

        if (finalAvailable > finalTotal)
            throw new IllegalArgumentException(
                    "Beschikbare exemplaren mogen niet groter zijn dan totaal aantal exemplaren");

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
        if (updatedBook.availableCopies() != null)
            book.setAvailableCopies(updatedBook.availableCopies());
        if (updatedBook.totalCopies() != null)
            book.setTotalCopies(updatedBook.totalCopies());
        if (updatedBook.readingLevel() != null)
            book.setReadingLevel(safeTrim(updatedBook.readingLevel()));
        if (updatedBook.ageRange() != null)
            book.setAgeRange(safeTrim(updatedBook.ageRange()));

        return toDTO(bookRepository.save(book));
    }

    public BookDTO addManualBook(CreateBookRequestDTO request) {
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

        BookEntity saved = bookRepository.saveAndFlush(book);

        // Reload zo dat de DB-gegenereerde ISBN in de response DTO zit
        BookEntity reloaded = bookRepository.findById(saved.getId())
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
                book.getAgeRange());
    }

    // Helper functies
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

}
