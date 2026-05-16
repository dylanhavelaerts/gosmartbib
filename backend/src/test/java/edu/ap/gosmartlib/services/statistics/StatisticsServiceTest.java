package edu.ap.gosmartlib.services.statistics;

import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookNotificationRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private LoanHistoryRepository loanHistoryRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private BookNotificationRepository bookNotificationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private static final String UID = "uid-1";
    private static final Long SCHOOL_ID = 5L;

    @BeforeEach
    void setUp() {
        SchoolEntity school = new SchoolEntity();
        school.setId(SCHOOL_ID);
        UserEntity user = new UserEntity();
        user.setSchool(school);
        lenient().when(userRepository.findDetailedBySmartschoolUid(UID)).thenReturn(Optional.of(user));
    }


    // --- getMostPopularBooks ---

    @Test
    void givenNoFilter_whenGetMostPopularBooks_thenPassesNullParams() {
        when(loanHistoryRepository.findMostPopularBooks(eq(SCHOOL_ID), any(), isNull(), isNull()))
                .thenReturn(List.of());

        statisticsService.getMostPopularBooks(UID, null, null);

        verify(loanHistoryRepository).findMostPopularBooks(eq(SCHOOL_ID), any(), isNull(), isNull());
    }

    @Test
    void givenClassNameFilter_whenGetMostPopularBooks_thenPassesClassName() {
        when(loanHistoryRepository.findMostPopularBooks(eq(SCHOOL_ID), any(), eq("3A"), isNull()))
                .thenReturn(List.of());

        statisticsService.getMostPopularBooks(UID, "3A", null);

        verify(loanHistoryRepository).findMostPopularBooks(eq(SCHOOL_ID), any(), eq("3A"), isNull());
    }

    @Test
    void givenGradeFilter_whenGetMostPopularBooks_thenPassesGrade() {
        when(loanHistoryRepository.findMostPopularBooks(eq(SCHOOL_ID), any(), isNull(), eq("3de jaar")))
                .thenReturn(List.of());

        statisticsService.getMostPopularBooks(UID, null, "3de jaar");

        verify(loanHistoryRepository).findMostPopularBooks(eq(SCHOOL_ID), any(), isNull(), eq("3de jaar"));
    }

    // --- getReturnPunctuality ---

    @Test
    void givenClassNameFilter_whenGetReturnPunctuality_thenPassesClassNameToAllRepos() {
        when(loanHistoryRepository.countOnTimeReturns(SCHOOL_ID, "3A", null)).thenReturn(10L);
        when(loanHistoryRepository.countLateReturns(SCHOOL_ID, "3A", null)).thenReturn(2L);
        when(loanRepository.countExtensionsByStatus(eq(SCHOOL_ID), any(), eq("3A"), isNull()))
                .thenReturn(List.of());

        statisticsService.getReturnPunctuality(UID, "3A", null);

        verify(loanHistoryRepository).countOnTimeReturns(SCHOOL_ID, "3A", null);
        verify(loanHistoryRepository).countLateReturns(SCHOOL_ID, "3A", null);
        verify(loanRepository).countExtensionsByStatus(eq(SCHOOL_ID), any(), eq("3A"), isNull());
    }

    // --- getOverviewStats ---

    @Test
    void givenGradeFilter_whenGetOverviewStats_thenPassesGradeToAllRepos() {
        when(loanRepository.countActiveLoansForSchool(SCHOOL_ID, null, "3de jaar")).thenReturn(5L);
        when(loanRepository.countOverdueLoansForSchool(eq(SCHOOL_ID), any(), isNull(), eq("3de jaar"))).thenReturn(1L);
        when(userRepository.countInactiveStudents(eq(SCHOOL_ID), any(), isNull(), eq("3de jaar"))).thenReturn(3L);
        when(loanRepository.countPendingExtensionsForSchool(SCHOOL_ID, null, "3de jaar")).thenReturn(0L);

        statisticsService.getOverviewStats(UID, null, "3de jaar");

        verify(loanRepository).countActiveLoansForSchool(SCHOOL_ID, null, "3de jaar");
        verify(loanRepository).countOverdueLoansForSchool(eq(SCHOOL_ID), any(), isNull(), eq("3de jaar"));
        verify(userRepository).countInactiveStudents(eq(SCHOOL_ID), any(), isNull(), eq("3de jaar"));
        verify(loanRepository).countPendingExtensionsForSchool(SCHOOL_ID, null, "3de jaar");
    }

    @Test
    void givenBothFilters_whenGetOverviewStats_thenPassesBothToAllRepos() {
        when(loanRepository.countActiveLoansForSchool(SCHOOL_ID, "3A", "3de jaar")).thenReturn(5L);
        when(loanRepository.countOverdueLoansForSchool(eq(SCHOOL_ID), any(), eq("3A"), eq("3de jaar"))).thenReturn(1L);
        when(userRepository.countInactiveStudents(eq(SCHOOL_ID), any(), eq("3A"), eq("3de jaar"))).thenReturn(3L);
        when(loanRepository.countPendingExtensionsForSchool(SCHOOL_ID, "3A", "3de jaar")).thenReturn(0L);

        statisticsService.getOverviewStats(UID, "3A", "3de jaar");

        verify(loanRepository).countActiveLoansForSchool(SCHOOL_ID, "3A", "3de jaar");
        verify(loanRepository).countOverdueLoansForSchool(eq(SCHOOL_ID), any(), eq("3A"), eq("3de jaar"));
        verify(userRepository).countInactiveStudents(eq(SCHOOL_ID), any(), eq("3A"), eq("3de jaar"));
        verify(loanRepository).countPendingExtensionsForSchool(SCHOOL_ID, "3A", "3de jaar");
    }

    // --- resolveSchoolId ---

    @Test
    void givenUnknownUser_whenGetMostPopularBooks_thenThrowsNotFound() {
        assertThrows(ResponseStatusException.class,
                () -> statisticsService.getMostPopularBooks("unknown", null, null));
    }
}
