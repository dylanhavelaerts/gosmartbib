package edu.ap.gosmartlib.controllers.statistics;

import edu.ap.gosmartlib.dto.statistics.BookPopularityDTO;
import edu.ap.gosmartlib.dto.statistics.ClassReadingStatsDTO;
import edu.ap.gosmartlib.dto.statistics.GenreStatsDTO;
import edu.ap.gosmartlib.dto.statistics.LoanDurationStatsDTO;
import edu.ap.gosmartlib.dto.statistics.LoansPerMonthDTO;
import edu.ap.gosmartlib.dto.statistics.MostWantedBookDTO;
import edu.ap.gosmartlib.dto.statistics.OverviewStatsDTO;
import edu.ap.gosmartlib.dto.statistics.ReturnPunctualityDTO;
import edu.ap.gosmartlib.dto.statistics.TopReaderStudentDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final AuthHelper authHelper;

    @GetMapping("/overview")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<OverviewStatsDTO> overview(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getOverviewStats(uid, className, grade));
    }

    @GetMapping("/popular-books")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<BookPopularityDTO>> popularBooks(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getMostPopularBooks(uid, className, grade));
    }

    @GetMapping("/popular-genres")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<GenreStatsDTO>> popularGenres(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getMostReadGenres(uid, className, grade));
    }

    @GetMapping("/highest-count-class")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<ClassReadingStatsDTO>> highestCountClass(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getMostReadingClasses(uid));
    }

    @GetMapping("/return-punctuality")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<ReturnPunctualityDTO> returnPunctuality(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getReturnPunctuality(uid, className, grade));
    }

    @GetMapping("/loan-duration-distribution")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<LoanDurationStatsDTO>> loanDurationDistribution(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getLoanDurationDistribution(uid, className, grade));
    }

    @GetMapping("/most-wanted-books")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<MostWantedBookDTO>> mostWantedBooks(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getMostWantedBooks(uid));
    }

    @GetMapping("/top-readers")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<TopReaderStudentDTO>> topReaders(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getTopReaders(uid, className, grade));
    }

    @GetMapping("/loans-per-month")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<LoansPerMonthDTO>> loansPerMonth(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String grade) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getLoansPerMonth(uid, className, grade));
    }

    @GetMapping("/least-popular-books")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<List<BookPopularityDTO>> leastPopularBooks(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getLeastPopularBooks(uid));
    }
}
