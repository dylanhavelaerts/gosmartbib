package edu.ap.gosmartlib.services.statistics;

import edu.ap.gosmartlib.dto.statistics.PersonalReadingStatDTO;
import edu.ap.gosmartlib.dto.statistics.ReaderProfileDTO;
import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.loan.LoanHistoryEntity;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.repositories.loan.LoanHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceTest {

    @Mock private LoanHistoryRepository loanHistoryRepository;
    @Mock private BookRepository bookRepository;

    @InjectMocks private UserStatsService userStatsService;

    private static final String UID = "student-uid";

    @Test
    void givenUser_whenGetPersonalReadingStats_thenReturnsDTOWithCorrectValues() {
        when(loanHistoryRepository.sumQuantityBySmartschoolUserId(UID)).thenReturn(7L);
        when(loanHistoryRepository.sumPagesByUid(UID)).thenReturn(1500L);
        when(loanHistoryRepository.findTopGenreForUser(eq(UID), any(PageRequest.class)))
                .thenReturn(List.of(new Object[]{"Fictie"}, new Object[]{"Avontuur"}));

        PersonalReadingStatDTO result = userStatsService.getPersonalReadingStats(UID);

        assertEquals(7, result.totalBooksRead());
        assertEquals(1500L, result.totalPagesRead());
        assertEquals(List.of("Fictie", "Avontuur"), result.topGenres());
    }

    @Test
    void givenUserWithNoLoans_whenGetPersonalReadingStats_thenReturnsZeroTotals() {
        when(loanHistoryRepository.sumQuantityBySmartschoolUserId(UID)).thenReturn(0L);
        when(loanHistoryRepository.sumPagesByUid(UID)).thenReturn(0L);
        when(loanHistoryRepository.findTopGenreForUser(eq(UID), any(PageRequest.class)))
                .thenReturn(List.of());

        PersonalReadingStatDTO result = userStatsService.getPersonalReadingStats(UID);

        assertEquals(0, result.totalBooksRead());
        assertEquals(0L, result.totalPagesRead());
        assertTrue(result.topGenres().isEmpty());
    }

    @Test
    void givenFewerThanFiveBooks_whenGetReaderProfile_thenReturnsNullProfileWithBooksNeeded() {
        when(loanHistoryRepository.findBySmartschoolUserId(UID)).thenReturn(
                List.of(buildLoan("isbn-1", LocalDate.of(2024, 1, 10)))
        );

        ReaderProfileDTO result = userStatsService.getReaderProfile(UID);

        assertNull(result.profileType());
        assertNull(result.profileLabel());
        assertEquals(1, result.booksRead());
        assertEquals(4, result.booksNeeded());
    }

    @Test
    void givenZeroBooks_whenGetReaderProfile_thenReturnsNullProfileWithFiveBooksNeeded() {
        when(loanHistoryRepository.findBySmartschoolUserId(UID)).thenReturn(List.of());

        ReaderProfileDTO result = userStatsService.getReaderProfile(UID);

        assertNull(result.profileType());
        assertEquals(0, result.booksRead());
        assertEquals(5, result.booksNeeded());
    }

    @Test
    void givenHighGenreVariety_whenGetReaderProfile_thenReturnsAvonturier() {
        // 9 unique genres → avonturier = 1.0; return dates 60 days apart → sprinter = 0.25; all popular → pionier = 0; 100 pages → titan = 0.33
        List<LoanHistoryEntity> history = List.of(
                buildLoan("isbn-1", LocalDate.of(2024, 1, 1)),
                buildLoan("isbn-2", LocalDate.of(2024, 3, 1)),
                buildLoan("isbn-3", LocalDate.of(2024, 5, 1)),
                buildLoan("isbn-4", LocalDate.of(2024, 7, 1)),
                buildLoan("isbn-5", LocalDate.of(2024, 9, 1))
        );
        when(loanHistoryRepository.findBySmartschoolUserId(UID)).thenReturn(history);
        when(bookRepository.findByIsbnIn(anyList())).thenReturn(List.of(
                buildBook("isbn-1", 100, List.of("Gen1", "Gen2", "Gen3")),
                buildBook("isbn-2", 100, List.of("Gen4", "Gen5", "Gen6")),
                buildBook("isbn-3", 100, List.of("Gen7", "Gen8", "Gen9")),
                buildBook("isbn-4", 100, List.of()),
                buildBook("isbn-5", 100, List.of())
        ));
        when(loanHistoryRepository.countLoansByIsbnIn(anyList())).thenReturn(List.of(
                new Object[]{"isbn-1", 10L}, new Object[]{"isbn-2", 10L},
                new Object[]{"isbn-3", 10L}, new Object[]{"isbn-4", 10L}, new Object[]{"isbn-5", 10L}
        ));

        ReaderProfileDTO result = userStatsService.getReaderProfile(UID);

        assertEquals("AVONTURIER", result.profileType());
        assertEquals("Avonturier", result.profileLabel());
        assertEquals(5, result.booksRead());
        assertEquals(0, result.booksNeeded());
    }

    @Test
    void givenHighAveragePageCount_whenGetReaderProfile_thenReturnsTitan() {
        // 400 pages avg → titan = 1.0; 1 genre → avonturier = 0.11; all popular → pionier = 0; spread dates → sprinter = 0.25
        List<LoanHistoryEntity> history = List.of(
                buildLoan("isbn-1", LocalDate.of(2024, 1, 1)),
                buildLoan("isbn-2", LocalDate.of(2024, 3, 1)),
                buildLoan("isbn-3", LocalDate.of(2024, 5, 1)),
                buildLoan("isbn-4", LocalDate.of(2024, 7, 1)),
                buildLoan("isbn-5", LocalDate.of(2024, 9, 1))
        );
        when(loanHistoryRepository.findBySmartschoolUserId(UID)).thenReturn(history);
        when(bookRepository.findByIsbnIn(anyList())).thenReturn(List.of(
                buildBook("isbn-1", 400, List.of("Gen1")),
                buildBook("isbn-2", 400, List.of("Gen1")),
                buildBook("isbn-3", 400, List.of("Gen1")),
                buildBook("isbn-4", 400, List.of("Gen1")),
                buildBook("isbn-5", 400, List.of("Gen1"))
        ));
        when(loanHistoryRepository.countLoansByIsbnIn(anyList())).thenReturn(List.of(
                new Object[]{"isbn-1", 10L}, new Object[]{"isbn-2", 10L},
                new Object[]{"isbn-3", 10L}, new Object[]{"isbn-4", 10L}, new Object[]{"isbn-5", 10L}
        ));

        ReaderProfileDTO result = userStatsService.getReaderProfile(UID);

        assertEquals("TITAN", result.profileType());
        assertEquals("Titaan", result.profileLabel());
    }

    @Test
    void givenMostlyLowPopularityBooks_whenGetReaderProfile_thenReturnsPionier() {
        // All 5 ISBNs have 1 school-wide loan (<5) → lowPopCount = 5, pionier = min(5/2.5, 1.0) = 1.0
        // 1 genre → avonturier = 0.11; 100 pages → titan = 0.33; spread dates → sprinter = 0.25
        List<LoanHistoryEntity> history = List.of(
                buildLoan("isbn-1", LocalDate.of(2024, 1, 1)),
                buildLoan("isbn-2", LocalDate.of(2024, 3, 1)),
                buildLoan("isbn-3", LocalDate.of(2024, 5, 1)),
                buildLoan("isbn-4", LocalDate.of(2024, 7, 1)),
                buildLoan("isbn-5", LocalDate.of(2024, 9, 1))
        );
        when(loanHistoryRepository.findBySmartschoolUserId(UID)).thenReturn(history);
        when(bookRepository.findByIsbnIn(anyList())).thenReturn(List.of(
                buildBook("isbn-1", 100, List.of("Gen1")),
                buildBook("isbn-2", 100, List.of("Gen1")),
                buildBook("isbn-3", 100, List.of("Gen1")),
                buildBook("isbn-4", 100, List.of("Gen1")),
                buildBook("isbn-5", 100, List.of("Gen1"))
        ));
        when(loanHistoryRepository.countLoansByIsbnIn(anyList())).thenReturn(List.of(
                new Object[]{"isbn-1", 1L}, new Object[]{"isbn-2", 1L},
                new Object[]{"isbn-3", 1L}, new Object[]{"isbn-4", 1L}, new Object[]{"isbn-5", 1L}
        ));

        ReaderProfileDTO result = userStatsService.getReaderProfile(UID);

        assertEquals("PIONIER", result.profileType());
        assertEquals("Pionier", result.profileLabel());
    }

    @Test
    void givenManyBooksInShortTimeWindow_whenGetReaderProfile_thenReturnsSprinter() {
        // 5 books returned within 30 days → max window count = 5, sprinter = 1.0
        // 1 genre → avonturier = 0.11; 100 pages → titan = 0.33; all popular → pionier = 0
        List<LoanHistoryEntity> history = List.of(
                buildLoan("isbn-1", LocalDate.of(2024, 1, 1)),
                buildLoan("isbn-2", LocalDate.of(2024, 1, 5)),
                buildLoan("isbn-3", LocalDate.of(2024, 1, 10)),
                buildLoan("isbn-4", LocalDate.of(2024, 1, 20)),
                buildLoan("isbn-5", LocalDate.of(2024, 1, 28))
        );
        when(loanHistoryRepository.findBySmartschoolUserId(UID)).thenReturn(history);
        when(bookRepository.findByIsbnIn(anyList())).thenReturn(List.of(
                buildBook("isbn-1", 100, List.of("Gen1")),
                buildBook("isbn-2", 100, List.of("Gen1")),
                buildBook("isbn-3", 100, List.of("Gen1")),
                buildBook("isbn-4", 100, List.of("Gen1")),
                buildBook("isbn-5", 100, List.of("Gen1"))
        ));
        when(loanHistoryRepository.countLoansByIsbnIn(anyList())).thenReturn(List.of(
                new Object[]{"isbn-1", 10L}, new Object[]{"isbn-2", 10L},
                new Object[]{"isbn-3", 10L}, new Object[]{"isbn-4", 10L}, new Object[]{"isbn-5", 10L}
        ));

        ReaderProfileDTO result = userStatsService.getReaderProfile(UID);

        assertEquals("SPRINTER", result.profileType());
        assertEquals("Sprinter", result.profileLabel());
    }

    @Test
    void givenNoUsersInSchool_whenGetProfileDistribution_thenReturnsAllZeroCounts() {
        when(loanHistoryRepository.findDistinctUserIdsBySchoolId(1L)).thenReturn(List.of());

        Map<String, Integer> result = userStatsService.getProfileDistribution(1L);

        assertEquals(0, result.get("AVONTURIER"));
        assertEquals(0, result.get("PIONIER"));
        assertEquals(0, result.get("SPRINTER"));
        assertEquals(0, result.get("TITAN"));
    }

    @Test
    void givenSchoolWithUserBelowMinBooks_whenGetProfileDistribution_thenAllPercentagesAreZero() {
        when(loanHistoryRepository.findDistinctUserIdsBySchoolId(1L)).thenReturn(List.of(UID));
        when(loanHistoryRepository.findBySmartschoolUserId(UID)).thenReturn(
                List.of(buildLoan("isbn-1", LocalDate.of(2024, 1, 1)))
        );

        Map<String, Integer> result = userStatsService.getProfileDistribution(1L);

        assertEquals(0, result.get("AVONTURIER"));
        assertEquals(0, result.get("PIONIER"));
        assertEquals(0, result.get("SPRINTER"));
        assertEquals(0, result.get("TITAN"));
    }

    private LoanHistoryEntity buildLoan(String isbn, LocalDate returnDate) {
        LoanHistoryEntity loan = new LoanHistoryEntity();
        loan.setIsbn(isbn);
        loan.setReturnDate(returnDate);
        loan.setLoanDate(returnDate.minusDays(14));
        loan.setDueDate(returnDate.minusDays(1));
        loan.setQuantity(1);
        return loan;
    }

    private BookEntity buildBook(String isbn, int pageCount, List<String> categories) {
        BookEntity book = new BookEntity();
        book.setIsbn(isbn);
        book.setPageCount(pageCount);
        book.setCategories(new ArrayList<>(categories));
        return book;
    }
}
