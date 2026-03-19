package edu.ap.testbackend.controllers;

import edu.ap.testbackend.dto.BookDTO;
import edu.ap.testbackend.dto.CreateBookRequestDTO;
import edu.ap.testbackend.dto.importdto.BulkImportResponseDTO;
import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.services.BookService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/books")
@CrossOrigin(origins = "*")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
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
            @RequestParam(defaultValue = "20") int size) {
        return bookService.getAllBooks(page, size);
    }

    @GetMapping("/all/unpaged")
    public List<BookDTO> getAllBooksUnpaged() {
        return bookService.getAllBooksUnpaged();
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
    public ResponseEntity<Page<BookDTO>> searchByTitleOrAuthor(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.searchByTitleOrAuthor(query, page, size));
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
            @RequestParam(required = false) Integer minPageCount,
            @RequestParam(required = false) Integer maxPageCount,
            @RequestParam(required = false) Integer minPubYear,
            @RequestParam(required = false) Integer maxPubYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<BookDTO> filteredBooks = bookService.filterBooks(
                    language, categories, minPageCount, maxPageCount, minPubYear, maxPubYear, page, size);
            return ResponseEntity.ok(filteredBooks);
        } catch (IllegalArgumentException e) {
            // 400 als de filter combinatie ongeldig is
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
    public BookDTO getBookById(@PathVariable Long id) throws BookNotFoundException {
        return bookService.getBookById(id);
    }

    @GetMapping("/spotlight")
    public List<BookDTO> getBooksInSpotlight() {
        return bookService.getTop4BooksInSpotlight();
    }

    @GetMapping("/spotlight/all")
    public List<BookDTO> getAllBooksInSpotlight() {
        return bookService.getAllBooksInSpotlight();
    }

    @GetMapping("/latest")
    public List<BookDTO> getLatestBooks() {
        return bookService.getLatestBooks();
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) throws BookNotFoundException {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Past de spotlight status aan van een boek.
     */
    @PatchMapping("/book/{id}/spotlight")
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

}
