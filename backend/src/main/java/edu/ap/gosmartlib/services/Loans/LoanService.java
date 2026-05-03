package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.BookNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.entities.BookInventoryEntity;

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
    private final UserRepository userRepository;
    private final BookNotificationService bookNotificationService;
    private final LoanPolicyRepository loanPolicyRepository;

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    // --- BOEKEN UITLENEN ---
    public void createLoans(List<LoanRequestDTO> loanRequests) {
        for (LoanRequestDTO request : loanRequests) {
            
            BookEntity book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new BookNotFoundException(request.bookId()));

            // 1. Zoek de gebruiker op in de databank (Crasht hier als de lener lokaal niet bestaat)
            UserEntity borrower = userRepository.findBySmartschoolUid(request.user().smartschoolUserId())
                    .orElseThrow(() -> new IllegalArgumentException("FOUT 1: Lener (" + request.user().smartschoolUserId() + ") is niet gevonden in de lokale databank."));

            // Check of de lener wel een school heeft (Voorkomt een NullPointerException)
            if (borrower.getSchool() == null) {
                throw new IllegalArgumentException("FOUT 2: De lener met ID " + request.user().smartschoolUserId() + " heeft geen school gekoppeld in de database.");
            }
            Long schoolId = borrower.getSchool().getId();

            // 2. Zoek de specifieke voorraad van het boek voor deze school
            if (book.getInventories() == null) {
                throw new IllegalArgumentException("FOUT 3: Het boek '" + book.getTitle() + "' heeft nog geen enkele voorraad (inventories) in de database.");
            }

            BookInventoryEntity inventory = book.getInventories().stream()
                    .filter(inv -> inv.getSchool() != null && inv.getSchool().getId().equals(schoolId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "FOUT 4: Boek '" + book.getTitle() + "' heeft geen voorraad toegewezen gekregen voor School ID: " + schoolId));

            // 3. Controleer of er genoeg voorraad is in de specifieke school
            if (inventory.getAvailableCopies() < request.quantity()) {
                throw new IllegalArgumentException(
                        "Niet genoeg exemplaren beschikbaar voor boek: " + book.getTitle());
            }

            // 4. Update de voorraad (zowel de school-voorraad als de totale voorraad)
            inventory.setAvailableCopies(inventory.getAvailableCopies() - request.quantity());
            book.setAvailableCopies(book.getAvailableCopies() - request.quantity());
            bookRepository.save(book);

            int loanPeriod = findReturnPeriodPerSchool(borrower);

            // 5. Zet in de uitleen tabel
            LoanEntity loan = new LoanEntity();
            loan.setSmartschoolUserId(request.user().smartschoolUserId());
            loan.setIsbn(book.getIsbn());
            loan.setQuantity(request.quantity());
            loan.setLoanDate(LocalDate.now());
            loan.setDueDate(LocalDate.now().plusDays(loanPeriod));

            loanRepository.save(loan);
            
            logger.info("UITLEEN GELOGD: {} exemplaren van ISBN {} uitgeleend aan gebruiker {} (School ID: {})", 
                        request.quantity(), book.getIsbn(), request.user().smartschoolUserId(), schoolId);
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
        history.setQuantity(returnQuantity);
        history.setLoanDate(loan.getLoanDate());
        history.setDueDate(loan.getDueDate());
        history.setReturnDate(LocalDate.now());
        loanHistoryRepository.save(history);

        // 2. Verhoog de voorraad in de boeken tabel én de school-inventory
        bookRepository.findByIsbn(loan.getIsbn()).ifPresent(book -> {
            book.setAvailableCopies(book.getAvailableCopies() + returnQuantity);
            bookRepository.save(book);

            userRepository.findBySmartschoolUid(loan.getSmartschoolUserId())
                    .ifPresent(borrower -> {
                        Long schoolId = borrower.getSchool().getId();
                        book.getInventories().stream()
                                .filter(inv -> inv.getSchool().getId().equals(schoolId))
                                .findFirst()
                                .ifPresent(inv -> {
                                    boolean bookHadZeroAvailableCopies = inv.getAvailableCopies() == 0;
                                    inv.setAvailableCopies(inv.getAvailableCopies() + returnQuantity);
                                    if (bookHadZeroAvailableCopies) {
                                        bookNotificationService.triggerNotificationsForBook(book, schoolId);
                                    }
                                });
                    });
        });

        // 3. Update of verwijder de actieve uitleen
        if (returnQuantity == loan.getQuantity()) {
            loanRepository.delete(loan);
            logger.info("RETOUR GELOGD: Alle {} exemplaren van ISBN {} teruggebracht door {}. Uitleen verwijderd.",
                    returnQuantity, loan.getIsbn(), loan.getSmartschoolUserId());
        } else {
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
                loan.getDueDate(),
                safeBook
            );
        }).toList();
    }

    // --- HELPER: VIND DE DEFAULT RETOUR PERIODE PER SCHOOL ---
    private int findReturnPeriodPerSchool(UserEntity borrower) {
        if (borrower.getSchool() == null) {
            throw new IllegalArgumentException("De lener heeft geen school gekoppeld in de database.");
        }

        Long schoolId = borrower.getSchool().getId();

        return loanPolicyRepository.findBySchool_Id(schoolId)
                .map(policy -> policy.getDefaultLoanPeriodDays())
                .orElse(14); // geen default? Standaard 14 dagen dat geselecteerd wordt
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