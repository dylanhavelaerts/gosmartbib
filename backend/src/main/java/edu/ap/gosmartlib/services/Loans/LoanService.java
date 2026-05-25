package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanExtensionRequestDTO;
import edu.ap.gosmartlib.dto.loan.LoanHistoryDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus;
import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.messages.BookNotificationService;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.entities.BookInventoryEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final UserDirectoryService userDirectoryService;

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    // --- BOEKEN UITLENEN ---
    public void createLoans(List<LoanRequestDTO> loanRequests) {
        for (LoanRequestDTO request : loanRequests) {

            BookEntity book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new BookNotFoundException(request.bookId()));

            // 1. Zoek de gebruiker op in de databank (Crasht hier als de lener lokaal niet
            // bestaat)
            UserEntity borrower = userRepository.findBySmartschoolUid(request.user().smartschoolUserId())
                    .orElseThrow(() -> new IllegalArgumentException("FOUT 1: Lener ("
                            + request.user().smartschoolUserId() + ") is niet gevonden in de lokale databank."));
            if (book.isDidacticTag() && borrower.getRole() == UserRoles.STUDENT) {
                throw new IllegalArgumentException(
                        "Didactische boeken kunnen niet worden uitgeleend aan leerlingen.");
            }
            // Check of de lener wel een school heeft (Voorkomt een NullPointerException)
            if (borrower.getSchool() == null) {
                throw new IllegalArgumentException("FOUT 2: De lener met ID " + request.user().smartschoolUserId()
                        + " heeft geen school gekoppeld in de database.");
            }
            Long schoolId = borrower.getSchool().getId();

            // 2. Zoek de specifieke voorraad van het boek voor deze school
            if (book.getInventories() == null) {
                throw new IllegalArgumentException("FOUT 3: Het boek '" + book.getTitle()
                        + "' heeft nog geen enkele voorraad (inventories) in de database.");
            }

            BookInventoryEntity inventory = book.getInventories().stream()
                    .filter(inv -> inv.getSchool() != null && inv.getSchool().getId().equals(schoolId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "FOUT 4: Boek '" + book.getTitle()
                                    + "' heeft geen voorraad toegewezen gekregen voor School ID: " + schoolId));

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
            loan.setExtensionStatus(LoanExtensionStatus.NONE);

            loanRepository.save(loan);

            logger.info("UITLEEN GELOGD: {} exemplaren van ISBN {} uitgeleend aan gebruiker {} (School ID: {})",
                    request.quantity(), book.getIsbn(), request.user().smartschoolUserId(), schoolId);
        }
    }

    // --- BOEKEN TERUGBRENGEN ---
    @Caching(evict = {
        @CacheEvict(value = "achievements", allEntries = true),
        @CacheEvict(value = "profileDistribution", allEntries = true)
    })
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
                        if (borrower.getSchool() == null) return;
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
    public List<ActiveLoanDTO> getActiveLoansAsAdmin(String actorUid, String targetUid) {
        requireBibliotheekbeheerder(actorUid); // gooit exception als de caller geen beheerder is
        return getActiveLoansByUser(targetUid);
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
                // FORCEER HIBERNATE OM DE DATA OP TE HALEN:
                // We maken een nieuwe ArrayList. Hierdoor dwingen we Hibernate om de auteurs
                // nu meteen uit de database te halen, vóórdat de transactie sluit.
                List<String> safeAuthors = book.getAuthors() != null
                        ? new java.util.ArrayList<>(book.getAuthors())
                        : new java.util.ArrayList<>();

                safeBook = new ActiveLoanDTO.LoanBookDTO(
                        book.getId(),
                        book.getTitle(),
                        book.getThumbnail(),
                        book.getIsbn(),
                        safeAuthors // <-- Gebruik hier de veilige kopie!
                );
            }

            String extensionStatus = loan.getExtensionStatus() != null
                    ? loan.getExtensionStatus().name()
                    : LoanExtensionStatus.NONE.name();

            return new ActiveLoanDTO(
                    loan.getId(),
                    loan.getSmartschoolUserId(),
                    loan.getQuantity(),
                    loan.getLoanDate(),
                    loan.getDueDate(),
                    extensionStatus,
                    safeBook);
        }).toList();
    }

    // --- VERLENGING AANVRAGEN: student/leerkracht ---
    public void requestLoanExtension(Long loanId, String actorUid) {
        UserEntity actor = userRepository.findBySmartschoolUid(actorUid)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker niet gevonden."));

        if (actor.getRole() != UserRoles.STUDENT && actor.getRole() != UserRoles.TEACHER) {
            throw new IllegalArgumentException("Alleen leerlingen en leerkrachten kunnen een verlenging aanvragen.");
        }

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Uitleen-record niet gevonden."));

        if (!loan.getSmartschoolUserId().equals(actorUid)) {
            throw new IllegalArgumentException("Je kunt alleen een verlenging aanvragen voor je eigen uitleningen.");
        }

        LoanExtensionStatus currentStatus = loan.getExtensionStatus() != null
                ? loan.getExtensionStatus()
                : LoanExtensionStatus.NONE;

        if (currentStatus == LoanExtensionStatus.PENDING) {
            throw new IllegalArgumentException("Er staat al een verlengingsaanvraag open voor deze lening.");
        }

        if (currentStatus == LoanExtensionStatus.APPROVED) {
            throw new IllegalArgumentException("Deze lening werd al verlengd.");
        }

        if (currentStatus == LoanExtensionStatus.DENIED) {
            throw new IllegalArgumentException(
                    "Deze verlengingsaanvraag werd al geweigerd. Je kunt geen nieuwe aanvraag indienen.");
        }

        loan.setExtensionStatus(LoanExtensionStatus.PENDING);
        loan.setExtensionRequestedAt(LocalDateTime.now());
        loan.setExtensionDecidedAt(null);
        loan.setExtensionDecidedBySmartschoolUserId(null);

        loanRepository.save(loan);
    }

    // --- VERLENGINGSAANVRAGEN VOOR EIGEN SCHOOL OPHALEN ---
    @Transactional(readOnly = true)
    public List<LoanExtensionRequestDTO> getPendingExtensionRequestsForSchool(String actorUid) {
        UserEntity actor = requireBibliotheekbeheerder(actorUid);
        Long schoolId = actor.getSchool().getId();

        List<LoanEntity> loans = loanRepository.findExtensionRequestsForSchool(
                LoanExtensionStatus.PENDING,
                schoolId);

        List<String> borrowerUids = loans.stream()
                .map(LoanEntity::getSmartschoolUserId)
                .filter(uid -> uid != null && !uid.isBlank())
                .distinct()
                .toList();

        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, borrowerUids);

        return loans.stream()
                .map(loan -> toLoanExtensionRequestDTO(loan, displayNames))
                .toList();
    }

    // --- VERLENGING GOEDKEUREN ---
    public void approveLoanExtension(Long loanId, String actorUid) {
        requireBibliotheekbeheerder(actorUid);

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Uitleen-record niet gevonden."));

        assertLoanBelongsToActorSchool(loan, actorUid);

        if (loan.getExtensionStatus() != LoanExtensionStatus.PENDING) {
            throw new IllegalArgumentException("Deze lening heeft geen open verlengingsaanvraag.");
        }

        loan.setDueDate(calculateExtendedDueDate(loan));
        loan.setExtensionStatus(LoanExtensionStatus.APPROVED);
        loan.setExtensionDecidedAt(LocalDateTime.now());
        loan.setExtensionDecidedBySmartschoolUserId(actorUid);

        loanRepository.save(loan);
    }

    // --- VERLENGING WEIGEREN ---
    public void denyLoanExtension(Long loanId, String actorUid) {
        requireBibliotheekbeheerder(actorUid);

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Uitleen-record niet gevonden."));

        assertLoanBelongsToActorSchool(loan, actorUid);

        if (loan.getExtensionStatus() != LoanExtensionStatus.PENDING) {
            throw new IllegalArgumentException("Deze lening heeft geen open verlengingsaanvraag.");
        }

        loan.setExtensionStatus(LoanExtensionStatus.DENIED);
        loan.setExtensionDecidedAt(LocalDateTime.now());
        loan.setExtensionDecidedBySmartschoolUserId(actorUid);

        loanRepository.save(loan);
    }

    private UserEntity requireBibliotheekbeheerder(String actorUid) {
        UserEntity actor = userRepository.findBySmartschoolUid(actorUid)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker niet gevonden."));

        if (actor.getSchool() == null) {
            throw new IllegalArgumentException("De gebruiker heeft geen school gekoppeld in de database.");
        }

        if (actor.getRole() != UserRoles.BIBLIOTHEEKBEHEERDER) {
            throw new IllegalArgumentException("Alleen bibliotheekbeheerders kunnen verlengingsaanvragen beheren.");
        }

        return actor;
    }

    private void assertLoanBelongsToActorSchool(LoanEntity loan, String actorUid) {
        UserEntity actor = requireBibliotheekbeheerder(actorUid);

        UserEntity borrower = userRepository.findBySmartschoolUid(loan.getSmartschoolUserId())
                .orElseThrow(() -> new IllegalArgumentException("Lener niet gevonden."));

        if (borrower.getSchool() == null || !borrower.getSchool().getId().equals(actor.getSchool().getId())) {
            throw new IllegalArgumentException("Je mag alleen aanvragen van je eigen school beheren.");
        }
    }

    private LocalDate calculateExtendedDueDate(LoanEntity loan) {
        long currentLoanPeriodDays = ChronoUnit.DAYS.between(loan.getLoanDate(), loan.getDueDate());

        if (currentLoanPeriodDays < 1) {
            currentLoanPeriodDays = 1;
        }

        return loan.getDueDate().plusDays(currentLoanPeriodDays);
    }

    private LoanExtensionRequestDTO toLoanExtensionRequestDTO(
            LoanEntity loan,
            Map<String, String> displayNames) {

        UserEntity borrower = userRepository.findBySmartschoolUid(loan.getSmartschoolUserId())
                .orElseThrow(() -> new IllegalArgumentException("Lener niet gevonden."));

        BookEntity book = bookRepository.findByIsbn(loan.getIsbn()).orElse(null);

        LoanExtensionRequestDTO.LoanBookDTO safeBook = null;

        if (book != null) {
            List<String> safeAuthors = book.getAuthors() != null
                    ? new java.util.ArrayList<>(book.getAuthors())
                    : new java.util.ArrayList<>();

            safeBook = new LoanExtensionRequestDTO.LoanBookDTO(
                    book.getId(),
                    book.getTitle(),
                    book.getThumbnail(),
                    book.getIsbn(),
                    safeAuthors);
        }

        return new LoanExtensionRequestDTO(
                loan.getId(),
                loan.getSmartschoolUserId(),
                resolveBorrowerDisplayName(borrower, displayNames),
                borrower.getRole(),
                loan.getQuantity(),
                loan.getLoanDate(),
                loan.getDueDate(),
                calculateExtendedDueDate(loan),
                loan.getExtensionRequestedAt(),
                safeBook);
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
    @Caching(evict = {
        @CacheEvict(value = "achievements", allEntries = true),
        @CacheEvict(value = "profileDistribution", allEntries = true)
    })
    public void returnBooksBulk(List<ReturnBulkRequestDTO> returnRequests) {
        for (ReturnBulkRequestDTO request : returnRequests) {
            // 1. Zoek op welk ISBN bij dit bookId hoort
            BookEntity book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new BookNotFoundException(request.bookId()));

            // 2. Haal ALLE actieve leningen op van deze user voor dit specifieke boek
            List<LoanEntity> activeLoans = loanRepository.findBySmartschoolUserIdAndIsbn(
                    request.smartschoolUserId(), book.getIsbn());

            // 3. Sorteer ze op inleverdatum (dichtstbijzijnde datum eerst)
            activeLoans.sort(Comparator.comparing(LoanEntity::getDueDate));

            // Hoeveel moeten er in totaal worden teruggebracht?
            int remainingToReturn = request.quantity();

            // 4. Loop door de leningen en schrijf ze af
            for (LoanEntity loan : activeLoans) {
                if (remainingToReturn <= 0)
                    break; // Alle teruggebrachte exemplaren zijn verwerkt!

                // Bepaal hoeveel we van DEZE specifieke record kunnen afhalen
                // (Kies de kleinste van de twee: wat we nog moeten inleveren vs wat er in deze
                // record staat)
                int returnForThisLoan = Math.min(remainingToReturn, loan.getQuantity());

                // 5. Hergebruik jouw bestaande logica om het in de geschiedenis te zetten!
                returnBook(loan.getId(), returnForThisLoan);

                // Verminder het aantal dat we nog moeten afhandelen
                remainingToReturn -= returnForThisLoan;
            }

            if (remainingToReturn > 0) {
                logger.warn(
                        "Let op: Frontend vroeg om {} '{}' boeken terug te brengen, maar de lener had er te weinig in bezit.",
                        remainingToReturn, book.getTitle());
            }
        }
    }

    // --- HISTORIEK OPHALEN ---
    public List<LoanHistoryDTO> getLoanHistoryByUser(String smartschoolUid) {
        List<LoanHistoryEntity> historyList = loanHistoryRepository
                .findBySmartschoolUserIdOrderByReturnDateDesc(smartschoolUid);

        return historyList.stream().map(history -> {
            LoanHistoryDTO dto = new LoanHistoryDTO();
            dto.setId(history.getId());

            // Omdat history enkel een ISBN opslaat en geen BookEntity,
            // moeten we het boek even opzoeken via de repository.
            BookEntity book = bookRepository.findByIsbn(history.getIsbn()).orElse(null);

            if (book != null) {
                dto.setBookTitle(book.getTitle());
                // In jullie BookEntity is authors een List<String>, we maken hier een mooie
                // string van
                dto.setAuthor(String.join(", ", book.getAuthors()));
            } else {
                // Fallback voor als het boek intussen verwijderd zou zijn uit de databank
                dto.setBookTitle("Onbekend Boek (ISBN: " + history.getIsbn() + ")");
                dto.setAuthor("Onbekende Auteur");
            }

            dto.setLoanDate(history.getLoanDate());
            dto.setReturnDate(history.getReturnDate());
            dto.setQuantity(history.getQuantity());
            return dto;
        }).collect(Collectors.toList());
    }
    public Page<LoanHistoryDTO> getLoanHistoryByUser(String smartschoolUid, Pageable pageable) {
        Page<LoanHistoryEntity> historyPage = loanHistoryRepository
                .findBySmartschoolUserIdOrderByReturnDateDesc(smartschoolUid, pageable);

        return historyPage.map(history -> {
            LoanHistoryDTO dto = new LoanHistoryDTO();
            dto.setId(history.getId());

            BookEntity book = bookRepository.findByIsbn(history.getIsbn()).orElse(null);
            if (book != null) {
                dto.setBookTitle(book.getTitle());
                dto.setAuthor(String.join(", ", book.getAuthors()));
            } else {
                dto.setBookTitle("Onbekend Boek (ISBN: " + history.getIsbn() + ")");
                dto.setAuthor("Onbekende Auteur");
            }

            dto.setLoanDate(history.getLoanDate());
            dto.setReturnDate(history.getReturnDate());
            dto.setQuantity(history.getQuantity());
            return dto;
        });
    }


    private Map<String, String> resolveDisplayNamesMap(String actorUid, List<String> uids) {
        if (uids == null || uids.isEmpty()) {
            return Map.of();
        }

        try {
            var response = userDirectoryService.resolveDisplayNames(
                    actorUid,
                    new ResolveDisplayNamesRequest(uids, null));

            if (response == null || !response.success() || response.displayNames() == null) {
                return Map.of();
            }

            return response.displayNames();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String resolveBorrowerDisplayName(UserEntity borrower, Map<String, String> displayNames) {
        String borrowerUid = borrower.getSmartschoolUid();

        if (borrowerUid != null && !borrowerUid.isBlank()) {
            String resolvedName = displayNames.get(borrowerUid);

            if (resolvedName != null && !resolvedName.isBlank()) {
                return resolvedName;
            }
        }

        return fallbackBorrowerLabel(borrower);
    }

    private String fallbackBorrowerLabel(UserEntity borrower) {
        if (borrower.getRole() == UserRoles.STUDENT) {
            return "Leerling";
        }

        if (borrower.getRole() == UserRoles.TEACHER) {
            return "Leerkracht";
        }

        return "Gebruiker";
    }
}