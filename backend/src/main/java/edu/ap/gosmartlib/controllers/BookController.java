package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.*;
import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.BookService;
import edu.ap.gosmartlib.services.users.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final UserService userService;
    private final AuthHelper authHelper;



    /**
     * Geeft alle boeken terug met server-side paginatie.
     *
     * @param page de pagina (0-based)
     * @param size aantal boeken per pagina
     */
    @GetMapping("/all")
    public Page<BookDTO> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAllBooks(page, size, callerRole(principal), authHelper.extractUidOrNull(principal));
    }

    @GetMapping("/all/unpaged")
    public List<BookDTO> getAllBooksUnpaged(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAllBooksUnpaged(callerRole(principal), authHelper.extractUidOrNull(principal));
    }

    /**
     * Zoekt boeken op titel of auteur met server-side paginatie.
     * Als de zoekterm leeg is, worden alle boeken teruggegeven.
     *
     * @param query de zoekterm
     * @param page  de pagina (0-based)
     * @param size  aantal boeken per pagina
     */
    @GetMapping("/search")
    public ResponseEntity<Page<BookDTO>> searchByTitleOrAuthorOrCategory(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(bookService.searchByTitleOrAuthorOrCategory(query, page, size, callerRole(principal),
                authHelper.extractUidOrNull(principal)));
    }

    /**
     * Filtert boeken op basis van verschillende criteria met server-side paginatie.
     * Alle filterparameters zijn optioneel.
     * Ongeldige combinaties (bv. minRating > maxRating) geven een 400 terug.
     *
     * @param filter    de filtercriteria (taal, categorieën, labels, pagina's,
     *                  publicatiejaar, beoordeling)
     * @param principal de ingelogde gebruiker
     */
    @GetMapping("/filter")
    public ResponseEntity<?> filterBooks(
            @ModelAttribute BookFilterRequest filter,
            @AuthenticationPrincipal OAuth2User principal) {
        Page<BookDTO> filteredBooks = bookService.filterBooks(filter, callerRole(principal),
                authHelper.extractUidOrNull(principal));
        return ResponseEntity.ok(filteredBooks);
    }

    @GetMapping("/languages")
    public List<String> getAvailableLanguages(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAvailableLanguages(authHelper.extractUidOrNull(principal));
    }

    /**
     * Geeft het boek terug met het opgegeven id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(bookService.getBookById(id, callerRole(principal), authHelper.extractUidOrNull(principal)));
    }

    @GetMapping("/spotlight")
    public List<BookDTO> getBooksInSpotlight(
            @RequestParam(required = false) String readingLevel,
            @AuthenticationPrincipal OAuth2User principal) {
        UserRoles role = callerRole(principal);
        String uid = authHelper.extractUidOrNull(principal);

        if (readingLevel == null || readingLevel.isBlank()) {
            return bookService.getTop4BooksInSpotlight(role, uid);
        }

        return bookService.getTop4BooksInSpotlight(role, uid, readingLevel);
    }

    public List<BookDTO> getBooksInSpotlight(OAuth2User principal) {
        return getBooksInSpotlight(null, principal);
    }

    /**
     * Geeft alle boeken terug met spotlight = true
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @GetMapping("/spotlight/all")
    public List<BookDTO> getAllBooksInSpotlight(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAllBooksInSpotlight(callerRole(principal), authHelper.extractUidOrNull(principal));
    }

    /**
     * Geeft de 4 boeken terug met de hoogste ID per leesniveau
     */
    @GetMapping("/latest")
    public List<BookDTO> getLatestBooks(
            @RequestParam(required = false) String readingLevel,
            @AuthenticationPrincipal OAuth2User principal) {
        UserRoles role = callerRole(principal);
        String uid = authHelper.extractUidOrNull(principal);

        if (readingLevel == null || readingLevel.isBlank()) {
            return bookService.getLatestBooks(role, uid);
        }

        return bookService.getLatestBooks(role, uid, readingLevel);
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<BookDTO>> getRecommendedBooks(
            @RequestParam(required = false) String readingLevel,
            @AuthenticationPrincipal OAuth2User principal) {
        UserRoles role = callerRole(principal);
        String uid = authHelper.extractUidOrNull(principal);

        if (readingLevel != null && !readingLevel.isBlank()) {
            return ResponseEntity.ok(bookService.getTopRatedBooksByReadingLevel(role, uid, readingLevel));
        }

        return ResponseEntity.ok(bookService.getRecommendedBooksForUser(authHelper.extractUid(principal)));
    }

    /**
     * Voegt een boek toe aan de database via ISBN (opgehaald van Google Books).
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PostMapping("/add/{isbn}")
    public ResponseEntity<?> addBookByIsbn(@PathVariable String isbn,
                                           @RequestParam(required = false) String campus,
                                           @RequestParam(required = false) Integer amount,
                                           @AuthenticationPrincipal OAuth2User principal) {
        BookDTO addedBook = bookService.addBookByIsbn(isbn, authHelper.extractUidOrNull(principal), campus, amount);
        return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
    }


    /**
     * Zoekt een boek op via ISBN zonder het op te slaan (preview).
     */
    @GetMapping("/search/{isbn}")
    public ResponseEntity<?> searchBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.searchBookByIsbn(isbn));
    }

    /**
     * Past de spotlight status aan van een boek.
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PatchMapping("/{id}/spotlight")
    public ResponseEntity<Void> updateSpotlight(
            @PathVariable Long id,
            @RequestParam boolean value) {
        bookService.updateSpotlight(id, value);
        return ResponseEntity.noContent().build();
    }

    /**
     * Importeert boeken vanuit een Excel-bestand.
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importBooks(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String campus,
            @RequestParam(defaultValue = "false") boolean confirmDuplicates,
            @RequestParam(required = false) List<Integer> confirmedDuplicateRows,
            @AuthenticationPrincipal OAuth2User principal) {
        try {
            BulkImportResponseDTO result = bookService.importBooksFromExcel(
                    file,
                    authHelper.extractUidOrNull(principal),
                    campus,
                    confirmDuplicates,
                    confirmedDuplicateRows == null ? List.of() : confirmedDuplicateRows);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Er is een fout opgetreden bij het importeren van het Excelbestand."));
        }
    }

    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PostMapping(value = "/import/no-isbn", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResponseDTO> importBooksWithoutIsbn(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String campus,
            @RequestParam(defaultValue = "false") boolean confirmDuplicates,
            @RequestParam(required = false) List<Integer> confirmedDuplicateRows,
            @AuthenticationPrincipal OAuth2User principal) {

        BulkImportResponseDTO result = bookService.importBooksWithoutIsbnFromExcel(
                file,
                authHelper.extractUid(principal),
                campus,
                confirmDuplicates,
                confirmedDuplicateRows == null ? List.of() : confirmedDuplicateRows);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody BookDTO bookDTO) {
        return ResponseEntity.ok(bookService.updateBook(id, bookDTO));
    }

    /**
     * Voegt boek toe aan database
     */
    @PostMapping("/add")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<?> addManualBook(@RequestBody CreateBookRequestDTO request,
                                           @AuthenticationPrincipal OAuth2User principal) {
        BookDTO addedBook = bookService.addManualBook(request, authHelper.extractUidOrNull(principal));
        return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/snowball")
    public ResponseEntity<List<SnowballSectionDTO>> getSnowball(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(
                bookService.getSnowballSections(id, callerRole(principal), authHelper.extractUidOrNull(principal)));
    }

    @GetMapping("/{bookId}/copies/labels")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<BookCopyLabelDTO>> getCopyLabels(@PathVariable Long bookId, @RequestParam Long inventoryId) {
        return ResponseEntity.ok(bookService.getCopyLabelsForInventory(bookId, inventoryId));
    }

    @GetMapping("/{bookId}/copies/labels/school")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<BookCopyLabelDTO>> getCopyLabelsForBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(bookService.getCopyLabelsForBook(bookId, authHelper.extractUid(principal)));
    }

    @GetMapping("/copies/labels/school")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<BookCopyLabelDTO>> getAllCopyLabelsForSchool(@AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(bookService.getCopyLabelsForSchool(authHelper.extractUid(principal)));
    }

    @GetMapping("/by-barcode")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<BookDTO> getBookByBarcode(
            @RequestParam String barcode,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(bookService.getBookByBarcode(barcode, callerRole(principal), authHelper.extractUidOrNull(principal)));
    }

    @GetMapping("/{bookId}/copies/by-barcode")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<BookCopyLabelDTO> getCopyByBarcode(@PathVariable Long bookId, @RequestParam String barcode) {
        return ResponseEntity.ok(bookService.getCopyByBarcode(bookId, barcode));
    }

    @PatchMapping("/copies/{copyId}/condition")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCopyCondition(@PathVariable Long copyId, @RequestBody UpdateCopyConditionRequest request) {
        bookService.updateCopyCondition(copyId, request.condition(), request.notes());
    }

    private UserRoles callerRole(OAuth2User principal) {
        if (principal == null)
            return UserRoles.STUDENT;
        String uid = principal.getAttribute("userID");
        if (uid == null)
            return UserRoles.STUDENT;
        return userService.getRoleBySmartschoolUid(uid);
    }

}
