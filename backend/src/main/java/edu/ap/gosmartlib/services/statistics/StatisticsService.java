package edu.ap.gosmartlib.services.statistics;

import edu.ap.gosmartlib.dto.statistics.BookPopularityDTO;
import edu.ap.gosmartlib.dto.statistics.ClassReadingStatsDTO;
import edu.ap.gosmartlib.dto.statistics.GenreStatsDTO;
import edu.ap.gosmartlib.dto.statistics.LoanDurationStatsDTO;
import edu.ap.gosmartlib.dto.statistics.LoansPerMonthDTO;
import edu.ap.gosmartlib.dto.statistics.MostWantedBookDTO;
import edu.ap.gosmartlib.dto.statistics.OverviewStatsDTO;
import edu.ap.gosmartlib.dto.statistics.ReturnPunctualityDTO;
import edu.ap.gosmartlib.dto.statistics.TopReaderStudentDTO;
import edu.ap.gosmartlib.entities.BookEntities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.bookRepositories.BookNotificationRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final LoanHistoryRepository loanHistoryRepository;
    private final LoanRepository loanRepository;
    private final BookNotificationRepository bookNotificationRepository;
    private final UserRepository userRepository;

    /**
     * Haal de meest populaire boeken op basis van het aantal uitleningen.
     * Optioneel te filteren op klas en/of jaar.
     *
     * @param uid       smartschool-uid van de ingelogde gebruiker, nodig om per school te filteren
     * @param className naam van de klas (nullable)
     * @param grade     jaar/graad (nullable)
     * @return top 10 populairste boeken
     */
    public List<BookPopularityDTO> getMostPopularBooks(String uid, String className, String grade)
 {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findMostPopularBooks(schoolId, PageRequest.of(0, 10), className, grade)
                .stream()
                .map(row -> toBookPopularityDTO((BookEntity) row[0], (Long) row[1]))
                .toList();
    }

    /**
     * Haal de populairste genres op basis van het aantal uitleningen.
     * Optioneel te filteren op klas en/of jaar.
     *
     * @param uid       smartschool-uid van de ingelogde gebruiker
     * @param className naam van de klas (nullable)
     * @param grade     jaar/graad (nullable)
     * @return top 15 populairste genres
     */
    public List<GenreStatsDTO> getMostReadGenres(String uid, String className, String grade) {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findMostReadGenres(schoolId, PageRequest.of(0, 15), className, grade)
                .stream()
                .map(row -> toGenreStatsDTO((String) row[0], (Long) row[1]))
                .toList();
    }

    /**
     * Haal lijst van klassen die de meeste boeken hebben gelezen, op basis van reading history
     * @param uid - nodig om per school te filteren
     * @return - een lijst van klassen met hun respectievelijke aantal gelezen boeken, gesorteerd op aantal gelezen boeken
     */
    public List<ClassReadingStatsDTO> getMostReadingClasses(String uid) {
        Long schoolId = resolveSchoolId(uid);

        return loanHistoryRepository.findMostReadingClasses(schoolId)
                .stream()
                .map(row -> toClassReadingStatsDTO((String) row[0], (String) row[1], (String) row[2], (Long) row[3]))
                .toList();
    }

    /**
     * Haal op hoeveel boeken op tijd vs te laat zijn teruggebracht en hoeveel verlengingen er zijn aangevraagd.
     * Optioneel te filteren op klas en/of jaar.
     *
     * @param uid       smartschool-uid van de ingelogde gebruiker
     * @param className naam van de klas (nullable)
     * @param grade     jaar/graad (nullable)
     * @return telling van op-tijd, te laat en verlengingsstatus
     */
    public ReturnPunctualityDTO getReturnPunctuality(String uid, String className, String grade) {
        Long schoolId = resolveSchoolId(uid);
        long onTime = loanHistoryRepository.countOnTimeReturns(schoolId, className, grade);
        long late = loanHistoryRepository.countLateReturns(schoolId, className, grade);

        long approved = 0, pending = 0, denied = 0;
        for (Object[] row : loanRepository.countExtensionsByStatus(schoolId, LoanExtensionStatus.NONE, className, grade)) {
            LoanExtensionStatus status = (LoanExtensionStatus) row[0];
            long count = (Long) row[1];
            switch (status) {
                case APPROVED -> approved = count;
                case PENDING  -> pending  = count;
                case DENIED   -> denied   = count;
            }
        }
        return new ReturnPunctualityDTO(onTime, late, approved, pending, denied);
    }

    /**
     * Haal de verdeling op van het aantal dagen dat leerlingen over het terugbrengen doen.
     * Optioneel te filteren op klas en/of jaar.
     *
     * @param uid       smartschool-uid van de ingelogde gebruiker
     * @param className naam van de klas (nullable)
     * @param grade     jaar/graad (nullable)
     * @return lijst van (duurDagen, aantal), gesorteerd op meest voorkomend
     */
    public List<LoanDurationStatsDTO> getLoanDurationDistribution(String uid, String className, String grade) {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findLoanDurationDistribution(schoolId, className, grade)
                .stream()
                .map(row -> new LoanDurationStatsDTO(((Number) row[0]).intValue(), (Long) row[1]))
                .toList();
    }

    /**
     * Haal de meest gevraagde boeken op via meldingen (notifications)
     * @param uid - nodig om per school te filteren
     * @return - top 10 meest gewenste boeken op basis van het aantal notificaties
     */
    public List<MostWantedBookDTO> getMostWantedBooks(String uid) {
        Long schoolId = resolveSchoolId(uid);
        return bookNotificationRepository.findMostWantedBooks(schoolId, PageRequest.of(0, 10))
                .stream()
                .map(row -> {
                    BookEntity book = (BookEntity) row[0];
                    long count = (Long) row[1];
                    return new MostWantedBookDTO(book.getIsbn(), book.getTitle(), book.getAuthors(), count);
                })
                .toList();
    }

    public List<TopReaderStudentDTO> getTopReaders(String uid, String className, String grade) {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findTopReaders(schoolId, PageRequest.of(0, 10), className, grade)
                .stream()
                .map(row -> {
                    String studentUid = (String) row[0];
                    long loanCount = (Long) row[1];
                    boolean isAnonymous = userRepository.findBySmartschoolUid(studentUid)
                            .map(UserEntity::isAnonymousLeaderboard)
                            .orElse(false);
                    return new TopReaderStudentDTO(isAnonymous ? "Anoniem" : studentUid, loanCount);
                })
                .toList();
    }

    public List<LoansPerMonthDTO> getLoansPerMonth(String uid, String className, String grade) {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findLoansPerMonth(schoolId, className, grade)
                .stream()
                .map(row -> new LoansPerMonthDTO(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (Long) row[2]))
                .toList();
    }

    public List<BookPopularityDTO> getLeastPopularBooks(String uid) {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findLeastPopularBooks(schoolId, PageRequest.of(0, 10))
                .stream()
                .map(row -> toBookPopularityDTO((BookEntity) row[0], (Long) row[1]))
                .toList();
    }

    public OverviewStatsDTO getOverviewStats(String uid, String className, String grade) {
        Long schoolId = resolveSchoolId(uid);
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusWeeks(4);

        long activeLoans = loanRepository.countActiveLoansForSchool(schoolId, className, grade);
        long overdueLoans = loanRepository.countOverdueLoansForSchool(schoolId, today, className, grade);
        long inactiveStudents = userRepository.countInactiveStudents(schoolId, since, className, grade);
        long pendingExtensions = loanRepository.countPendingExtensionsForSchool(schoolId, className, grade);

        return new OverviewStatsDTO(activeLoans, overdueLoans, inactiveStudents, pendingExtensions);
    }

    private Long resolveSchoolId(String uid) {
        return userRepository.findDetailedBySmartschoolUid(uid)
                .map(u -> u.getSchool().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
    }

    private BookPopularityDTO toBookPopularityDTO(BookEntity book, Long loanCount) {
        return new BookPopularityDTO(
                book.getIsbn(),
                book.getTitle(),
                book.getAuthors(),
                loanCount
        );
    }

    private GenreStatsDTO toGenreStatsDTO(String genre, Long loanCount) {
        return new GenreStatsDTO(genre, loanCount);
    }

    private ClassReadingStatsDTO toClassReadingStatsDTO(String className, String grade, String schoolYear, Long loanCount) {
        return new ClassReadingStatsDTO(className, grade, schoolYear, loanCount);
    }
}
