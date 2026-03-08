package edu.ap.testbackend.controllers;

import edu.ap.testbackend.exceptions.BookNotFoundException;
import edu.ap.testbackend.services.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@CrossOrigin(origins = "*")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/all")
    public List<BookDTO> getBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/spotlight/all")
    public List<BookDTO> getAllBooksInSpotlight() {
        return bookService.getAllBooksInSpotlight();
    }

    @GetMapping("/spotlight")
    public List<BookDTO> getBooksInSpotlight() {
        return bookService.getTop4BooksInSpotlight();
    }

    @GetMapping("/latest")
    public List<BookDTO> getLatestBooks() {
        return bookService.getLatestBooks();
    }

    /**
     * Ontvangt het id via het URL-pad en geeft dit door aan de service.
     * Geeft de bijhorende BookDTO terug.
     */
    @GetMapping("/{id}")
    public BookDTO getBookById(@PathVariable Long id) throws BookNotFoundException {
        return bookService.getBookById(id);
    }

    /**
     * Ontvangt het id via het URL-pad en vraagt de service om het boek te
     * verwijderen.
     * Geeft een 204 No Content response terug bij succes.
     */
    @DeleteMapping("/book/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) throws BookNotFoundException {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/book/{id}/spotlight")
    public ResponseEntity<Void> updateSpotlight(
            @PathVariable Long id,
            @RequestParam boolean value) throws BookNotFoundException {
        bookService.updateSpotlight(id, value);
        return ResponseEntity.noContent().build();
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
