package edu.ap.gosmartlib.services.loans;

import edu.ap.gosmartlib.dto.loan.LibrarianActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LibrarianLoanHistoryDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.dto.userdirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.loan.LoanEntity;
import edu.ap.gosmartlib.entities.loan.LoanHistoryEntity;
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.repositories.loan.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.loan.LoanRepository;
import edu.ap.gosmartlib.repositories.school.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
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
    private final AdminLoanMapper mapper;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional(readOnly = true)
    public Page<LibrarianActiveLoanDTO> getActiveLoansForSchool(String actorUid, Long classId, int page, int size) {
        UserEntity actor = userRepository.findBySmartschoolUid(actorUid)
                .orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden."));
        if (actor.getSchool() == null)
            throw new IllegalArgumentException("De gebruiker heeft geen school gekoppeld.");
        Long schoolId = actor.getSchool().getId();
        PageRequest pageable = PageRequest.of(page, size);

        Page<LoanEntity> result = classId != null
                ? loanRepository.findAllActiveBySchoolIdAndClassId(schoolId, classId, pageable)
                : loanRepository.findAllActiveBySchoolId(schoolId, pageable);

        List<LoanEntity> loans = result.getContent();
        if (loans.isEmpty()) return Page.empty(pageable);

        List<String> uids = distinctNonBlank(loans.stream().map(LoanEntity::getSmartschoolUserId).toList());
        List<String> isbns = distinctNonBlank(loans.stream().map(LoanEntity::getIsbn).toList());
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, uids);
        Map<String, List<String>> classMap = buildClassMap(schoolId, uids);
        Map<String, BookEntity> bookMap = toBookMap(isbns);

        List<LibrarianActiveLoanDTO> content = loans.stream()
                .map(loan -> mapper.toActiveDTO(loan, displayNames, classMap, bookMap)).toList();
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<LibrarianLoanHistoryDTO> getLoanHistoryForSchool(String actorUid, Long classId, int page, int size) {
        UserEntity actor = userRepository.findBySmartschoolUid(actorUid)
                .orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden."));
        if (actor.getSchool() == null)
            throw new IllegalArgumentException("De gebruiker heeft geen school gekoppeld.");
        Long schoolId = actor.getSchool().getId();
        PageRequest pageable = PageRequest.of(page, size);

        Page<LoanHistoryEntity> result = classId != null
                ? loanHistoryRepository.findAllBySchoolIdAndClassIdOrderByReturnDateDesc(schoolId, classId, pageable)
                : loanHistoryRepository.findAllBySchoolIdOrderByReturnDateDesc(schoolId, pageable);

        List<LoanHistoryEntity> history = result.getContent();
        if (history.isEmpty()) return Page.empty(pageable);

        List<String> uids = distinctNonBlank(history.stream().map(LoanHistoryEntity::getSmartschoolUserId).toList());
        List<String> isbns = distinctNonBlank(history.stream().map(LoanHistoryEntity::getIsbn).toList());
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, uids);
        Map<String, List<String>> classMap = buildClassMap(schoolId, uids);
        Map<String, BookEntity> bookMap = toBookMap(isbns);

        List<LibrarianLoanHistoryDTO> content = history.stream()
                .map(h -> mapper.toHistoryDTO(h, displayNames, classMap, bookMap)).toList();
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }
    @Transactional(readOnly = true)
    public List<ReadingListAssignmentTargetsDTO.ClassTarget> getSchoolClasses(String actorUid) {
        UserEntity actor = userRepository.findBySmartschoolUid(actorUid)
                .orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden."));
        if (actor.getSchool() == null)
            throw new IllegalArgumentException("De gebruiker heeft geen school gekoppeld.");
        return schoolClassRepository.findAllBySchool_IdOrderByNameAsc(actor.getSchool().getId())
                .stream()
                .collect(Collectors.toMap(
                        SchoolClassEntity::getName,
                        c -> new ReadingListAssignmentTargetsDTO.ClassTarget(c.getId(), c.getName(), null, null),
                        (existing, duplicate) -> existing,  // bij duplicaatnaam: eerste houden
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    private Map<String, String> resolveDisplayNamesMap(String actorUid, List<String> uids) {
        if (uids.isEmpty()) return Map.of();
        try {
            var response = userDirectoryService.resolveDisplayNames(
                    actorUid, new ResolveDisplayNamesRequest(uids, null));
            if (response == null || !response.success() || response.displayNames() == null) return Map.of();
            return response.displayNames();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Map<String, List<String>> buildClassMap(Long schoolId, List<String> uids) {
        return userRepository.findAllBySchool_IdAndSmartschoolUidIn(schoolId, uids)
                .stream()
                .collect(Collectors.toMap(
                        UserEntity::getSmartschoolUid,
                        u -> u.getClasses().stream()
                                .map(SchoolClassEntity::getName)
                                .distinct()
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
