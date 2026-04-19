package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.BookDTO;
import edu.ap.gosmartlib.dto.CreateBookRequestDTO;
import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.BookService;
import edu.ap.gosmartlib.util.UserRoles;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/books")
@CrossOrigin(origins = "*")
public class BookController {
    private final BookService bookService;
    private final UserRepository userRepository;
    public BookController(BookService bookService,  UserRepository userRepository) {
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

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
            Authentication authentication)
    {
        return bookService.getAllBooks(page, size, callerRole(authentication));
    }

    @GetMapping("/all/unpaged")
    public List<BookDTO> getAllBooksUnpaged(Authentication authentication) {
        return bookService.getAllBooksUnpaged(callerRole(authentication));
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
            Authentication authentication) {
        return ResponseEntity.ok(bookService.searchByTitleOrAuthorOrCategory(query, page, size,callerRole(authentication)));
    }

    /**
     * Filtert boeken op taal, categorie, pagina's en publicatiejaar met server-side
     * paginatie.
     * 
     * @param page de pagina (0-based)
     * @param size aantal boeken per pagina
     */
    @GetMapping("/filter")
    public ResponseEntity<?> filterBooks(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> labels,
            @RequestParam(required = false) Integer minPageCount,
            @RequestParam(required = false) Integer maxPageCount,
            @RequestParam(required = false) Integer minPubYear,
            @RequestParam(required = false) Integer maxPubYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Page<BookDTO> filteredBooks = bookService.filterBooks(
                    language, categories, labels, minPageCount, maxPageCount, minPubYear, maxPubYear, page, size,callerRole(authentication));
            return ResponseEntity.ok(filteredBooks);
        } catch (IllegalArgumentException e) {
            // 400 als de filter combinatie Fongeldig is
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (DataAccessException e) {
            // 500 als de error op database niveau is
            return new ResponseEntity<>("An error occurred during filtering.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Geeft het boek terug met het opgegeven id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(bookService.getBookById(id, callerRole(authentication)));
        } catch (BookNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }

    /**
     * Geeft de 4 boeken terug met de hoogste ID en spotlight = true
     */
    @GetMapping("/spotlight")
    public List<BookDTO> getBooksInSpotlight(Authentication authentication) {
        return bookService.getTop4BooksInSpotlight(callerRole(authentication));
    }

    /**
     * Geeft alle boeken terug met spotlight = true
     */
    @GetMapping("/spotlight/all")
    public List<BookDTO> getAllBooksInSpotlight(Authentication authentication) {
        return bookService.getAllBooksInSpotlight(callerRole(authentication));
    }

    /**
     * Geeft de 4 boeken terug met de hoogste ID
     */
    @GetMapping("/latest")
    public List<BookDTO> getLatestBooks(Authentication authentication) {
        return bookService.getLatestBooks(callerRole(authentication));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<BookDTO>> getRecommendedBooks(
            Authentication authentication) {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String uid = principal.getAttribute("userID");
        return ResponseEntity.ok(bookService.getRecommendedBooksForUser(uid));
    }

    /**
     * Voegt een boek toe aan de database via ISBN (opgehaald van Google Books).
     */
    @PostMapping("/add/{isbn}")
    public ResponseEntity<?> addBookByIsbn(@PathVariable String isbn) {
        try {
            BookDTO addedBook = bookService.addBookByIsbn(isbn);
            return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while fetching the book.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Zoekt een boek op via ISBN zonder het op te slaan (preview).
     */
    @GetMapping("/search/{isbn}")
    public ResponseEntity<?> searchBookByIsbn(@PathVariable String isbn) {
        try {
            BookDTO bookPreview = bookService.searchBookByIsbn(isbn);
            return new ResponseEntity<>(bookPreview, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while fetching the book.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verwijdert een boek via het id.
     * Geeft 204 No Content terug bij succes.
     */
    @PreAuthorize("hasAnyRole('BIBLIOTHEEKBEHEERDER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) throws BookNotFoundException {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Past de spotlight status aan van een boek.
     */
    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    @PatchMapping("/{id}/spotlight")
    public ResponseEntity<Void> updateSpotlight(
            @PathVariable Long id,
            @RequestParam boolean value) throws BookNotFoundException {
        bookService.updateSpotlight(id, value);
        return ResponseEntity.noContent().build();
    }

    /**
     * Importeert boeken vanuit een Excel-bestand.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importBooks(@RequestParam("file") MultipartFile file) {
        try {
            BulkImportResponseDTO result = bookService.importBooksFromExcel(file);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while importing the Excel file.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('BIBLIOTHEEKBEHEERDER', 'ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody BookDTO bookDTO) {
        try {
            BookDTO updated = bookService.updateBook(id, bookDTO);
            return ResponseEntity.ok(updated);
        } catch (BookNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Voegt boek toe aan database
     */
    @PostMapping("/add")
    public ResponseEntity<?> addManualBook(@RequestBody CreateBookRequestDTO request) {
        try {
            BookDTO addedBook = bookService.addManualBook(request);
            return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while saving the book.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * helper methods
     */
    private UserRoles callerRole(Authentication authentication) {
        if (authentication == null) return UserRoles.STUDENT;

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String uid = principal.getAttribute("userID");

        return userRepository.findBySmartschoolUid(uid)
                .map(UserEntity::getRole)
                .orElse(UserRoles.STUDENT);
    }

}
