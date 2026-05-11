package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.dto.loan.AdminActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.AdminLoanHistoryDTO;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus;
import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.UnauthorizedRoleException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.UserDirectoryService;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminLoanService {

    private final LoanRepository loanRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final UserDirectoryService userDirectoryService;

    @Transactional(readOnly = true)
    public List<AdminActiveLoanDTO> getActiveLoansForSchool(String actorUid, Long classId) {
        UserEntity actor = requireBibliotheekbeheerder(actorUid);
        Long schoolId = actor.getSchool().getId();

        List<LoanEntity> loans = classId != null
                ? loanRepository.findAllActiveBySchoolIdAndClassId(schoolId, classId)
                : loanRepository.findAllActiveBySchoolId(schoolId);

        if (loans.isEmpty()) return List.of();

        List<String> uids = distinctNonBlank(loans.stream().map(LoanEntity::getSmartschoolUserId).toList());
        List<String> isbns = distinctNonBlank(loans.stream().map(LoanEntity::getIsbn).toList());

        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, uids);
        Map<String, List<String>> classMap = buildClassMap(schoolId, uids);
        Map<String, BookEntity> bookMap = toBookMap(isbns);

        return loans.stream().map(loan -> toActiveDTO(loan, displayNames, classMap, bookMap)).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminLoanHistoryDTO> getLoanHistoryForSchool(String actorUid, Long classId) {
        UserEntity actor = requireBibliotheekbeheerder(actorUid);
        Long schoolId = actor.getSchool().getId();

        List<LoanHistoryEntity> history = classId != null
                ? loanHistoryRepository.findAllBySchoolIdAndClassIdOrderByReturnDateDesc(schoolId, classId)
                : loanHistoryRepository.findAllBySchoolIdOrderByReturnDateDesc(schoolId);

        if (history.isEmpty()) return List.of();

        List<String> uids = distinctNonBlank(history.stream().map(LoanHistoryEntity::getSmartschoolUserId).toList());
        List<String> isbns = distinctNonBlank(history.stream().map(LoanHistoryEntity::getIsbn).toList());

        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, uids);
        Map<String, List<String>> classMap = buildClassMap(schoolId, uids);
        Map<String, BookEntity> bookMap = toBookMap(isbns);

        return history.stream().map(h -> toHistoryDTO(h, displayNames, classMap, bookMap)).toList();
    }

    private AdminActiveLoanDTO toActiveDTO(
            LoanEntity loan,
            Map<String, String> displayNames,
            Map<String, List<String>> classMap,
            Map<String, BookEntity> bookMap) {

        String uid = loan.getSmartschoolUserId();
        BookEntity book = bookMap.get(loan.getIsbn());
        String extensionStatus = loan.getExtensionStatus() != null
                ? loan.getExtensionStatus().name() : LoanExtensionStatus.NONE.name();

        return new AdminActiveLoanDTO(
                loan.getId(), uid,
                displayNames.getOrDefault(uid, "Leerling"),
                classMap.getOrDefault(uid, List.of()),
                loan.getQuantity(), loan.getLoanDate(), loan.getDueDate(),
                extensionStatus, toBookDTO(book));
    }

    private AdminLoanHistoryDTO toHistoryDTO(
            LoanHistoryEntity history,
            Map<String, String> displayNames,
            Map<String, List<String>> classMap,
            Map<String, BookEntity> bookMap) {

        String uid = history.getSmartschoolUserId();
        BookEntity book = bookMap.get(history.getIsbn());

        AdminLoanHistoryDTO dto = new AdminLoanHistoryDTO();
        dto.setId(history.getId());
        dto.setLoanDate(history.getLoanDate());
        dto.setReturnDate(history.getReturnDate());
        dto.setQuantity(history.getQuantity());
        dto.setBorrowerDisplayName(displayNames.getOrDefault(uid, "Leerling"));
        dto.setBorrowerClassNames(classMap.getOrDefault(uid, List.of()));

        if (book != null) {
            dto.setBookTitle(book.getTitle());
            dto.setAuthor(String.join(", ", book.getAuthors()));
        } else {
            dto.setBookTitle("Onbekend Boek (ISBN: " + history.getIsbn() + ")");
            dto.setAuthor("Onbekende Auteur");
        }

        return dto;
    }

    private AdminActiveLoanDTO.LoanBookDTO toBookDTO(BookEntity book) {
        if (book == null) return null;
        List<String> authors = book.getAuthors() != null
                ? new java.util.ArrayList<>(book.getAuthors()) : new java.util.ArrayList<>();
        return new AdminActiveLoanDTO.LoanBookDTO(
                book.getId(), book.getTitle(), book.getThumbnail(), book.getIsbn(), authors);
    }

    private UserEntity requireBibliotheekbeheerder(String actorUid) {
        UserEntity actor = userRepository.findBySmartschoolUid(actorUid)
                .orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden."));
        if (actor.getSchool() == null)
            throw new IllegalArgumentException("De gebruiker heeft geen school gekoppeld.");
        if (actor.getRole() != UserRoles.BIBLIOTHEEKBEHEERDER)
            throw new UnauthorizedRoleException("Alleen bibliotheekbeheerders hebben toegang tot dit overzicht.");
        return actor;
    }

    private Map<String, String> resolveDisplayNamesMap(String actorUid, List<String> uids) {
        if (uids.isEmpty()) return Map.of();
        try {
            var response = userDirectoryService.resolveDisplayNames(
                    actorUid, new ResolveDisplayNamesRequest(uids));
            if (response == null || !response.success() || response.displayNames() == null) return Map.of();
            return response.displayNames();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Map<String, List<String>> buildClassMap(Long schoolId, List<String> uids) {
        return userRepository.findAllBySchool_IdAndSmartschoolUidInAndActiveIsTrue(schoolId, uids)
                .stream()
                .collect(Collectors.toMap(
                        UserEntity::getSmartschoolUid,
                        u -> u.getClasses().stream()
                                .map(SchoolClassEntity::getName)
                                .sorted().toList()));
    }

    private Map<String, BookEntity> toBookMap(List<String> isbns) {
        return bookRepository.findByIsbnIn(isbns).stream()
                .collect(Collectors.toMap(BookEntity::getIsbn, b -> b));
    }

    private List<String> distinctNonBlank(List<String> values) {
        return values.stream().filter(v -> v != null && !v.isBlank()).distinct().toList();
    }
}



