package edu.ap.testbackend.controllers;

import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.services.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@CrossOrigin(origins = "*") // nog specifiekere CORS-instellingen toeveogen later
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/all")
    public List<BookDTO> getBooks() {
        return bookService.getAllBooks();
    }

    /**
     * Ontvangt het id via het URL-pad en geeft dit door aan de service.
     * Geeft de bijhorende BookDTO terug.
     */
    @GetMapping("/book/{id}")
    public BookDTO getBookById(@PathVariable Long id) throws BookNotFoundException {
        return bookService.getBookById(id);
    }

    /**
     * Ontvangt het id via het URL-pad en vraagt de service om het boek te verwijderen.
     * Geeft een 204 No Content response terug bij succes.
     */
    @DeleteMapping("/book/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) throws BookNotFoundException {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();

    }

    /**
     * Zoekt boeken op basis van een zoekterm.
     * Er wordt gezocht in zowel de titel als de auteurs van het boek.
     * Als de zoekterm leeg is, worden alle boeken teruggegeven.
     * @param query De zoekterm om op te filteren
     * @return Een lijst van boeken die overeenkomen met de zoekterm
     */
    @GetMapping("/search")
    public ResponseEntity<List<BookDTO>> searchByTitleOrAuthor(@RequestParam String query) {
        return ResponseEntity.ok(bookService.searchByTitleOrAuthor(query));
    }

    @PostMapping("/add/{isbn}")
    public ResponseEntity<?> addBookByIsbn(@PathVariable String isbn) {
        try {
            BookDTO addedBook = bookService.addBookByIsbn(isbn);
            return new ResponseEntity<>(addedBook, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Dit vangt de error op als Google Books geen resultaat heeft
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Dit vangt onverwachte server errors (zoals netwerkproblemen met Google)
            return new ResponseEntity<>("An error occurred while fetching the book.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/{isbn}")
    public ResponseEntity<?> searchBookByIsbn(@PathVariable String isbn) {
        try {
            // Zoek het boek op zonder op te slaan
            BookDTO bookPreview = bookService.searchBookByIsbn(isbn);
            return new ResponseEntity<>(bookPreview, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while fetching the book.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}