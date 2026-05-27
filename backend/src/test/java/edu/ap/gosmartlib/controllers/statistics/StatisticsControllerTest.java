package edu.ap.gosmartlib.controllers.statistics;

import edu.ap.gosmartlib.dto.statistics.*;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.statistics.StatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock private StatisticsService statisticsService;
    @Mock private AuthHelper authHelper;
    @Mock private OAuth2User principal;

    @InjectMocks
    private StatisticsController statisticsController;

    private static final String UID = "uid-1";

    // --- overview ---

    @Test
    void givenNoFilter_whenOverview_thenCallsServiceWithNullParams() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getOverviewStats(UID, null, null)).thenReturn(mock(OverviewStatsDTO.class));

        ResponseEntity<OverviewStatsDTO> response = statisticsController.overview(principal, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getOverviewStats(UID, null, null);
    }

    @Test
    void givenClassNameAndGrade_whenOverview_thenPassesBothToService() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getOverviewStats(UID, "3A", "3de jaar")).thenReturn(mock(OverviewStatsDTO.class));

        statisticsController.overview(principal, "3A", "3de jaar");

        verify(statisticsService).getOverviewStats(UID, "3A", "3de jaar");
    }

    // --- popularBooks ---

    @Test
    void givenNoFilter_whenPopularBooks_thenCallsServiceWithNullParams() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getMostPopularBooks(UID, null, null)).thenReturn(List.of());

        ResponseEntity<List<BookPopularityDTO>> response = statisticsController.popularBooks(principal, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getMostPopularBooks(UID, null, null);
    }

    @Test
    void givenClassNameFilter_whenPopularBooks_thenPassesClassNameToService() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getMostPopularBooks(UID, "3A", null)).thenReturn(List.of());

        statisticsController.popularBooks(principal, "3A", null);

        verify(statisticsService).getMostPopularBooks(UID, "3A", null);
    }

    // --- topReaders ---

    @Test
    void givenGradeFilter_whenTopReaders_thenPassesGradeToService() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getTopReaders(UID, null, "3de jaar")).thenReturn(List.of());

        statisticsController.topReaders(principal, null, "3de jaar");

        verify(statisticsService).getTopReaders(UID, null, "3de jaar");
    }

    // --- highestCountClass (geen filter) ---

    @Test
    void whenHighestCountClass_thenCallsServiceWithoutFilter() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getMostReadingClasses(UID)).thenReturn(List.of());

        ResponseEntity<List<ClassReadingStatsDTO>> response = statisticsController.highestCountClass(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getMostReadingClasses(UID);
        verify(statisticsService, never()).getMostPopularBooks(any(), any(), any());
    }

    // --- mostWantedBooks (geen filter) ---

    @Test
    void whenMostWantedBooks_thenCallsServiceWithoutFilter() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getMostWantedBooks(UID)).thenReturn(List.of());

        ResponseEntity<List<MostWantedBookDTO>> response = statisticsController.mostWantedBooks(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getMostWantedBooks(UID);
    }

    // --- popularGenres ---

    @Test
    void givenNoFilter_whenPopularGenres_thenCallsServiceWithNullParams() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getMostReadGenres(UID, null, null)).thenReturn(List.of());

        ResponseEntity<List<GenreStatsDTO>> response = statisticsController.popularGenres(principal, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getMostReadGenres(UID, null, null);
    }

    @Test
    void givenGradeFilter_whenPopularGenres_thenPassesGradeToService() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getMostReadGenres(UID, null, "3de jaar")).thenReturn(List.of());

        statisticsController.popularGenres(principal, null, "3de jaar");

        verify(statisticsService).getMostReadGenres(UID, null, "3de jaar");
    }

    // --- returnPunctuality ---

    @Test
    void givenNoFilter_whenReturnPunctuality_thenCallsServiceWithNullParams() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getReturnPunctuality(UID, null, null)).thenReturn(mock(ReturnPunctualityDTO.class));

        ResponseEntity<ReturnPunctualityDTO> response = statisticsController.returnPunctuality(principal, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getReturnPunctuality(UID, null, null);
    }

    @Test
    void givenClassNameFilter_whenReturnPunctuality_thenPassesClassNameToService() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getReturnPunctuality(UID, "3A", null)).thenReturn(mock(ReturnPunctualityDTO.class));

        statisticsController.returnPunctuality(principal, "3A", null);

        verify(statisticsService).getReturnPunctuality(UID, "3A", null);
    }

    // --- loanDurationDistribution ---

    @Test
    void givenNoFilter_whenLoanDurationDistribution_thenCallsServiceWithNullParams() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getLoanDurationDistribution(UID, null, null)).thenReturn(List.of());

        ResponseEntity<List<LoanDurationStatsDTO>> response = statisticsController.loanDurationDistribution(principal, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getLoanDurationDistribution(UID, null, null);
    }

    @Test
    void givenGradeFilter_whenLoanDurationDistribution_thenPassesGradeToService() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getLoanDurationDistribution(UID, null, "2de jaar")).thenReturn(List.of());

        statisticsController.loanDurationDistribution(principal, null, "2de jaar");

        verify(statisticsService).getLoanDurationDistribution(UID, null, "2de jaar");
    }

    // --- loansPerMonth ---

    @Test
    void givenNoFilter_whenLoansPerMonth_thenCallsServiceWithNullParams() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getLoansPerMonth(UID, null, null)).thenReturn(List.of());

        ResponseEntity<List<LoansPerMonthDTO>> response = statisticsController.loansPerMonth(principal, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getLoansPerMonth(UID, null, null);
    }

    @Test
    void givenClassAndGradeFilter_whenLoansPerMonth_thenPassesBothToService() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getLoansPerMonth(UID, "4B", "4de jaar")).thenReturn(List.of());

        statisticsController.loansPerMonth(principal, "4B", "4de jaar");

        verify(statisticsService).getLoansPerMonth(UID, "4B", "4de jaar");
    }

    // --- leastPopularBooks ---

    @Test
    void whenLeastPopularBooks_thenCallsServiceAndReturnsOk() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getLeastPopularBooks(UID)).thenReturn(List.of());

        ResponseEntity<List<BookPopularityDTO>> response = statisticsController.leastPopularBooks(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getLeastPopularBooks(UID);
    }

    // --- topReaders (extra combos) ---

    @Test
    void givenNoFilter_whenTopReaders_thenCallsServiceWithNullParams() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(statisticsService.getTopReaders(UID, null, null)).thenReturn(List.of());

        ResponseEntity<List<TopReaderStudentDTO>> response = statisticsController.topReaders(principal, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(statisticsService).getTopReaders(UID, null, null);
    }
}
