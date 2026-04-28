package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
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
import java.util.Comparator;

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

    // --- ACTIEVE LENINGEN OPHALEN ---
    public List<ActiveLoanDTO> getActiveLoansByUser(String smartschoolUserId) {
        List<LoanEntity> loans = loanRepository.findBySmartschoolUserId(smartschoolUserId);
        
        return loans.stream().map(loan -> {
            // Zoek het bijbehorende boek op via ISBN
            BookEntity book = bookRepository.findByIsbn(loan.getIsbn()).orElse(null);
            
            // Zet de zware BookEntity om naar een lichte LoanBookDTO
            ActiveLoanDTO.LoanBookDTO safeBook = null;
            if (book != null) {
                safeBook = new ActiveLoanDTO.LoanBookDTO(
                    book.getId(),
                    book.getTitle(),
                    book.getThumbnail(),
                    book.getIsbn()
                );
            }
            
            return new ActiveLoanDTO(
                loan.getId(), 
                loan.getSmartschoolUserId(), 
                loan.getQuantity(), 
                loan.getLoanDate(), 
                safeBook
            );
        }).toList();
    }

    // --- BULK BOEKEN TERUGBRENGEN (Vanuit Frontend Mandje) ---
    public void returnBooksBulk(List<ReturnBulkRequestDTO> returnRequests) {
        for (ReturnBulkRequestDTO request : returnRequests) {
            // 1. Zoek op welk ISBN bij dit bookId hoort
            BookEntity book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new BookNotFoundException(request.bookId()));
            
            // 2. Haal ALLE actieve leningen op van deze user voor dit specifieke boek
            List<LoanEntity> activeLoans = loanRepository.findBySmartschoolUserIdAndIsbn(
                    request.smartschoolUserId(), book.getIsbn()
            );

            // 3. Sorteer ze op inleverdatum (dichtstbijzijnde datum eerst)
            activeLoans.sort(Comparator.comparing(LoanEntity::getDueDate));

            // Hoeveel moeten er in totaal worden teruggebracht?
            int remainingToReturn = request.quantity();

            // 4. Loop door de leningen en schrijf ze af
            for (LoanEntity loan : activeLoans) {
                if (remainingToReturn <= 0) break; // Alle teruggebrachte exemplaren zijn verwerkt!

                // Bepaal hoeveel we van DEZE specifieke record kunnen afhalen
                // (Kies de kleinste van de twee: wat we nog moeten inleveren vs wat er in deze record staat)
                int returnForThisLoan = Math.min(remainingToReturn, loan.getQuantity());

                // 5. Hergebruik jouw bestaande logica om het in de geschiedenis te zetten!
                returnBook(loan.getId(), returnForThisLoan);

                // Verminder het aantal dat we nog moeten afhandelen
                remainingToReturn -= returnForThisLoan;
            }
            
            if (remainingToReturn > 0) {
                logger.warn("Let op: Frontend vroeg om {} '{}' boeken terug te brengen, maar de lener had er te weinig in bezit.", 
                            remainingToReturn, book.getTitle());
            }
        }
    }
}