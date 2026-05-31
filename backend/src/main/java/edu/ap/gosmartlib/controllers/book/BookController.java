package edu.ap.gosmartlib.controllers.book;

import edu.ap.gosmartlib.dto.*;
import edu.ap.gosmartlib.dto.book.BookCopyLabelDTO;
import edu.ap.gosmartlib.dto.book.BookDTO;
import edu.ap.gosmartlib.dto.book.BookFilterRequest;
import edu.ap.gosmartlib.dto.book.CreateBookRequestDTO;
import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.book.BookService;
import edu.ap.gosmartlib.services.users.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final UserService userService;
    private final AuthHelper authHelper;

    // region GET methodes

    /**
     * Haalt een gepagineerde lijst met boeken op die zichtbaar zijn voor de huidige
     * gebruiker.
     *
     * <p>
     * Standaard wordt rekening gehouden met de rol van de gebruiker. Leerlingen
     * zien
     * bijvoorbeeld geen didactische boeken, terwijl personeel die normaal wel mag
     * zien.
     * Wanneer {@code studentReadableOnly} true is, worden didactische boeken altijd
     * uitgesloten. Dit wordt gebruikt voor leeslijsten die bedoeld zijn voor
     * leerlingen.
     * </p>
     *
     * @param page                het paginanummer, startend vanaf 0
     * @param size                het aantal boeken per pagina
     * @param studentReadableOnly true wanneer enkel boeken getoond mogen worden die
     *                            geschikt zijn voor leerlingenleeslijsten
     * @param principal           de aangemelde gebruiker
     * @return een pagina met zichtbare boeken
     */
    @GetMapping("/all")
    public Page<BookDTO> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean studentReadableOnly,
            @AuthenticationPrincipal OAuth2User principal) {
        UserRoles role = callerRole(principal);
        String uid = authHelper.extractUidOrNull(principal);

        if (studentReadableOnly) {
            return bookService.getAllStudentReadableBooks(page, size, role, uid);
        }

        return bookService.getAllBooks(page, size, role, uid);
    }

    @GetMapping("/all/unpaged")
    public List<BookDTO> getAllBooksUnpaged(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAllBooksUnpaged(callerRole(principal), authHelper.extractUidOrNull(principal));
    }

    /**
     * Zoekt boeken op titel, auteur of categorie.
     *
     * <p>
     * De gewone zoekopdracht gebruikt de standaard zichtbaarheidsregels per rol.
     * Wanneer {@code studentReadableOnly} true is, worden didactische boeken altijd
     * uitgesloten, ook wanneer de aangemelde gebruiker normaal didactische boeken
     * mag zien.
     * Dit voorkomt dat didactische boeken gekozen kunnen worden voor leeslijsten
     * voor
     * leerlingen.
     * </p>
     *
     * @param query               de zoekterm voor titel, auteur of categorie
     * @param page                het paginanummer, startend vanaf 0
     * @param size                het aantal boeken per pagina
     * @param studentReadableOnly true wanneer enkel boeken getoond mogen worden die
     *                            geschikt zijn voor leerlingenleeslijsten
     * @param principal           de aangemelde gebruiker
     * @return een pagina met boeken die overeenkomen met de zoekterm
     */
    @GetMapping("/search")
    public ResponseEntity<Page<BookDTO>> searchByTitleOrAuthorOrCategory(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean studentReadableOnly,
            @AuthenticationPrincipal OAuth2User principal) {
        UserRoles role = callerRole(principal);
        String uid = authHelper.extractUidOrNull(principal);

        if (studentReadableOnly) {
            return ResponseEntity.ok(bookService.searchStudentReadableBooks(query, page, size, role, uid));
        }

        return ResponseEntity.ok(bookService.searchByTitleOrAuthorOrCategory(query, page, size, role, uid));
    }

    /**
     * Zoekt boeken op basis van verschillende filters zoals titel, auteur,
     * categorie, taal, leesniveau en labels
     * De resultaten worden gefilterd op basis van de rol van de gebruiker, zoals
     * bij bovenstaande methoden
     * 
     * @param filter    - een object dat alle mogelijke filtercriteria bevat
     * @param principal - de ingelogde gebruiker
     * @return - een pagina met boeken die overeenkomen met de filtercriteria en die
     *         de gebruiker mag zien
     */
    @GetMapping("/filter")
    public ResponseEntity<?> filterBooks(
            @ModelAttribute BookFilterRequest filter,
            @AuthenticationPrincipal OAuth2User principal) {
        Page<BookDTO> filteredBooks = bookService.filterBooks(filter, callerRole(principal),
                authHelper.extractUidOrNull(principal));
        return ResponseEntity.ok(filteredBooks);
    }

    /**
     * Geeft een lijst van beschikbare talen terug
     * 
     * @param principal - de ingelogde gebruiker
     * @return - een lijst van beschikbare talen
     */
    @GetMapping("/languages")
    public List<String> getAvailableLanguages(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAvailableLanguages(authHelper.extractUidOrNull(principal));
    }

    /**
     * Geeft een lijst van beschikbare categorieën terug
     * 
     * @param principal - de ingelogde gebruiker
     * @return - een lijst van beschikbare categorieën
     */
    @GetMapping("/categories")
    public List<String> getAvailableCategories(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAvailableCategories(authHelper.extractUidOrNull(principal));
    }

    /**
     * Geeft een lijst van beschikbare leesniveaus terug
     * 
     * @param principal - de ingelogde gebruiker
     * @return - een lijst van beschikbare leesniveaus
     */
    @GetMapping("/labels")
    public List<String> getAvailableLabels(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAvailableLabels(authHelper.extractUidOrNull(principal));
    }

    /**
     * Geeft het boek terug met het opgegeven id.
     * 
     * @param id        - het id van het boek
     * @param principal - de ingelogde gebruiker
     * @return - het boek met het opgegeven id
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity
                .ok(bookService.getBookById(id, callerRole(principal), authHelper.extractUidOrNull(principal)));
    }

    /**
     * Geeft de 4 boeken terug die in de spotlight staan, gesorteerd op rating.
     * Optioneel kunnen deze ook gefilterd worden op leesniveau
     * 
     * @param readingLevel - het leesniveau waarop gefilterd moet worden
     * @param principal    - de ingelogde gebruiker
     * @return - een lijst van maximaal 4 boeken die in de spotlight staan en
     *         overeenkomen met het leesniveau (indien opgegeven) en die de
     *         gebruiker mag zien
     */
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

    /**
     * Geeft de 4 nieuwste boeken terug, gesorteerd op toevoegdatum. Optioneel
     * kunnen deze ook gefilterd worden op leesniveau
     * 
     * @param readingLevel - het leesniveau waarop gefilterd moet worden
     * @param principal    - de ingelogde gebruiker
     * @return - een lijst van de 4 nieuwste boeken
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

    /**
     * Geeft de 4 best beoordeelde boeken terug, gesorteerd op gemiddelde rating.
     * Optioneel kunnen deze ook gefilterd worden op leesniveau
     * 
     * @param readingLevel - het leesniveau waarop gefilterd moet worden
     * @param principal    - de ingelogde gebruiker
     * @return - een lijst van de 4 best beoordeelde boeken die overeenkomen met het
     *         leesniveau (indien opgegeven) en die de gebruiker mag zien
     */
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
     * Zoekt een boek op via ISBN zonder het op te slaan (preview).
     * 
     * @param isbn - het ISBN van het boek dat gezocht moet worden
     * @return - het gevonden boek
     */
    @GetMapping("/search/{isbn}")
    public ResponseEntity<?> searchBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.searchBookByIsbn(isbn));
    }

    /**
     * Haalt de snowball-secties voor een specifiek boek op
     * 
     * @param id        - het ID van het boek
     * @param principal - de ingelogde gebruiker
     * @return - de snowball-secties
     */
    @GetMapping("/{id}/snowball")
    public ResponseEntity<List<SnowballSectionDTO>> getSnowball(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(
                bookService.getSnowballSections(id, callerRole(principal), authHelper.extractUidOrNull(principal)));
    }

    /**
     * Geeft alle boeken terug met spotlight = true
     * 
     * @param principal - de ingelogde gebruiker
     * @return - een lijst van boeken die in de spotlight staan
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @GetMapping("/spotlight/all")
    public List<BookDTO> getAllBooksInSpotlight(@AuthenticationPrincipal OAuth2User principal) {
        return bookService.getAllBooksInSpotlight(callerRole(principal), authHelper.extractUidOrNull(principal));
    }

    /**
     * Haalt een boek op basis van de barcode
     * 
     * @param barcode   - de barcode van het boek
     * @param principal - de ingelogde gebruiker
     * @return - het gevonden boek
     */
    @GetMapping("/by-barcode")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<BookDTO> getBookByBarcode(
            @RequestParam String barcode,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(
                bookService.getBookByBarcode(barcode, callerRole(principal), authHelper.extractUidOrNull(principal)));
    }

    /**
     * Haalt een kopie op basis van de barcode
     * 
     * @param bookId  - het ID van het boek
     * @param barcode - de barcode van de kopie
     * @return - de gevonden kopie
     */
    @GetMapping("/{bookId}/copies/by-barcode")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<BookCopyLabelDTO> getCopyByBarcode(@PathVariable Long bookId, @RequestParam String barcode) {
        return ResponseEntity.ok(bookService.getCopyByBarcode(bookId, barcode));
    }

    /**
     * Haalt de labels voor kopieën van een specifiek boek op
     * 
     * @param bookId      - het ID van het boek
     * @param inventoryId - het ID van het inventaris
     * @return - de labels voor de kopieën
     */
    @GetMapping("/{bookId}/copies/labels")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<BookCopyLabelDTO>> getCopyLabels(@PathVariable Long bookId,
            @RequestParam Long inventoryId) {
        return ResponseEntity.ok(bookService.getCopyLabelsForInventory(bookId, inventoryId));
    }

    /**
     * Haalt de labels voor kopieën van een specifiek boek op
     * 
     * @param bookId    - het ID van het boek
     * @param principal - de ingelogde gebruiker
     * @return - de labels voor de kopieën
     */
    @GetMapping("/{bookId}/copies/labels/school")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<BookCopyLabelDTO>> getCopyLabelsForBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(bookService.getCopyLabelsForBook(bookId, authHelper.extractUid(principal)));
    }

    /**
     * Haalt alle labels voor kopieën voor een specifieke school op
     * 
     * @param principal - de ingelogde gebruiker
     * @return - de labels voor de kopieën
     */
    @GetMapping("/copies/labels/school")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<BookCopyLabelDTO>> getAllCopyLabelsForSchool(
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(bookService.getCopyLabelsForSchool(authHelper.extractUid(principal)));
    }

    // endregion

    // region POST methodes

    /**
     * Voegt een boek toe aan de database op basis van het opgegeven ISBN.
     * De boekgegevens worden opgehaald uit een externe API
     * - Optioneel kunnen er ook campus, aantal exemplaren, en of het een didactisch
     * boek is meegegeven worden
     * 
     * @param isbn         - het ISBN van het boek dat toegevoegd moet worden
     * @param campus       - de campus waaraan het boek toegevoegd moet worden
     *                     (optioneel)
     * @param amount       - het aantal exemplaren dat toegevoegd moet worden
     *                     (optioneel, standaard 1)
     * @param didacticBook - of het boek een didactisch boek is (optioneel,
     *                     standaard false)
     * @param principal    - de ingelogde gebruiker
     * @return - het toegevoegde boek
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PostMapping("/add/{isbn}")
    public ResponseEntity<?> addBookByIsbn(@PathVariable String isbn,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) Integer amount,
            @RequestParam(defaultValue = "false") boolean didacticBook,
            @AuthenticationPrincipal OAuth2User principal) {
        BookDTO addedBook = bookService.addBookByIsbn(isbn, authHelper.extractUidOrNull(principal), campus, amount,
                didacticBook);
        return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
    }

    /**
     * Voegt een boek toe aan de database op basis van de opgegeven gegevens
     * 
     * @param request   - het verzoek met de gegevens van het boek
     * @param principal - de ingelogde gebruiker
     * @return - het toegevoegde boek
     */
    @PostMapping("/add")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<?> addManualBook(@RequestBody CreateBookRequestDTO request,
            @AuthenticationPrincipal OAuth2User principal) {
        BookDTO addedBook = bookService.addManualBook(request, authHelper.extractUidOrNull(principal));
        return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
    }

    /**
     * Importeert boeken vanuit een Excel-bestand.
     * 
     * @param file                   - het Excel-bestand met de boekgegevens
     * @param campus                 - de campus waaraan de boeken toegevoegd moeten
     *                               worden (optioneel)
     * @param confirmDuplicates      - of dubbele boeken bevestigd moeten worden
     *                               (optioneel, standaard false)
     * @param confirmedDuplicateRows - de rijen met dubbele boeken die bevestigd
     *                               zijn (optioneel)
     * @param principal              - de ingelogde gebruiker
     * @return - het resultaat van de import
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
            log.error("Onverwachte fout bij Excelimport met ISBN", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Er is een fout opgetreden bij het importeren van het Excelbestand."));
        }
    }

    /**
     * Importeert boeken zonder ISBN vanuit een Excel-bestand
     * 
     * @param file                   - het Excel-bestand met de boekgegevens
     * @param campus                 - de campus waaraan de boeken toegevoegd moeten
     *                               worden (optioneel)
     * @param confirmDuplicates      - of dubbele boeken bevestigd moeten worden
     *                               (optioneel, standaard false)
     * @param confirmedDuplicateRows - de rijen met dubbele boeken die bevestigd
     *                               zijn (optioneel)
     * @param principal              - de ingelogde gebruiker
     * @return - het resultaat van de import
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PostMapping(value = "/import/no-isbn", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importBooksWithoutIsbn(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String campus,
            @RequestParam(defaultValue = "false") boolean confirmDuplicates,
            @RequestParam(required = false) List<Integer> confirmedDuplicateRows,
            @AuthenticationPrincipal OAuth2User principal) {
        try {
            BulkImportResponseDTO result = bookService.importBooksWithoutIsbnFromExcel(
                    file,
                    authHelper.extractUid(principal),
                    campus,
                    confirmDuplicates,
                    confirmedDuplicateRows == null ? List.of() : confirmedDuplicateRows);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Onverwachte fout bij Excelimport zonder ISBN", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Er is een fout opgetreden bij het importeren van het Excelbestand."));
        }
    }

    // endregion

    // region PATCH methodes

    /**
     * Past de spotlight status aan van een boek.
     * 
     * @param id    - het ID van het boek waarvan de spotlight status moet worden
     *              aangepast
     * @param value - de nieuwe spotlight status
     * @return - een lege response met HTTP 204 No Content
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
     * Werkt een boek bij in de database
     * 
     * @param id      - het ID van het boek
     * @param bookDTO - de gegevens van het boek
     * @return - het bijgewerkte boek
     */
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody BookDTO bookDTO) {
        return ResponseEntity.ok(bookService.updateBook(id, bookDTO));
    }

    /**
     * Werkt de conditie van een kopie bij
     * 
     * @param copyId  - het ID van de kopie
     * @param request - de aanvraag met de nieuwe conditie en opmerkingen
     */
    @PatchMapping("/copies/{copyId}/condition")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCopyCondition(@PathVariable Long copyId, @RequestBody UpdateCopyConditionRequestDTO request) {
        bookService.updateCopyCondition(copyId, request.condition(), request.notes());
    }

    // endregion

    // region Helper methodes

    private UserRoles callerRole(OAuth2User principal) {
        if (principal == null)
            return UserRoles.STUDENT;
        String uid = principal.getAttribute("userID");
        if (uid == null)
            return UserRoles.STUDENT;
        return userService.getRoleBySmartschoolUid(uid);
    }

    // endregion

}
