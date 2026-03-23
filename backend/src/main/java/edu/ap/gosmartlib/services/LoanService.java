package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanHistoryEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final BookRepository bookRepository;
    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    // --- BOEKEN UITLENEN ---
    public void createLoans(List<LoanRequestDTO> loanRequests) {
        for (LoanRequestDTO request : loanRequests) {
            
            // AANGEPAST: Geef alleen request.bookId() (Long) door, geen String!
            BookEntity book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new BookNotFoundException(request.bookId()));

            if (book.getAvailableCopies() < request.quantity()) {
                throw new IllegalArgumentException("Niet genoeg exemplaren beschikbaar voor boek: " + book.getTitle());
            }

            // 1. Update de voorraad in de boeken tabel
            book.setAvailableCopies(book.getAvailableCopies() - request.quantity());
            bookRepository.save(book);

            // 2. Zet in de uitleen tabel per ISBN
            LoanEntity loan = new LoanEntity();
            loan.setSmartschoolUserId(request.user().smartschoolUserId());
            loan.setIsbn(book.getIsbn());
            loan.setQuantity(request.quantity());
            loan.setLoanDate(LocalDate.now());
            loan.setDueDate(LocalDate.now().plusDays(21)); // Standaard 3 weken de tijd

            loanRepository.save(loan);
            
            logger.info("UITLEEN GELOGD: {} exemplaren van ISBN {} uitgeleend aan gebruiker {}", 
                        request.quantity(), book.getIsbn(), request.user().smartschoolUserId());
        }
    }

    // --- BOEKEN TERUGBRENGEN ---
    public void returnBook(Long loanId, int returnQuantity) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Uitleen-record niet gevonden."));

        if (returnQuantity <= 0 || returnQuantity > loan.getQuantity()) {
            throw new IllegalArgumentException("Ongeldig aantal om terug te brengen.");
        }

        // 1. Voeg toe aan geschiedenis tabel
        LoanHistoryEntity history = new LoanHistoryEntity();
        history.setSmartschoolUserId(loan.getSmartschoolUserId());
        history.setIsbn(loan.getIsbn());
        history.setQuantity(returnQuantity); // Het aantal dat daadwerkelijk is teruggebracht
        history.setLoanDate(loan.getLoanDate());
        history.setReturnDate(LocalDate.now());
        loanHistoryRepository.save(history);

        // 2. Verhoog de voorraad in de boeken tabel
        bookRepository.findByIsbn(loan.getIsbn()).ifPresent(book -> {
            book.setAvailableCopies(book.getAvailableCopies() + returnQuantity);
            bookRepository.save(book);
        });

        // 3. Update of verwijder de actieve uitleen
        if (returnQuantity == loan.getQuantity()) {
            // Alle uitgeleende exemplaren zijn terug -> Verwijder de record
            loanRepository.delete(loan);
            logger.info("RETOUR GELOGD: Alle {} exemplaren van ISBN {} teruggebracht door {}. Uitleen verwijderd.", 
                        returnQuantity, loan.getIsbn(), loan.getSmartschoolUserId());
        } else {
            // Minder teruggebracht dan uitgeleend -> Update de quantity
            loan.setQuantity(loan.getQuantity() - returnQuantity);
            loanRepository.save(loan);
            logger.info("DEEL-RETOUR GELOGD: {} exemplaren van ISBN {} teruggebracht door {}. Nog {} uitgeleend.", 
                        returnQuantity, loan.getIsbn(), loan.getSmartschoolUserId(), loan.getQuantity());
        }
    }
}