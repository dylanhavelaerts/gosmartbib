package edu.ap.gosmartlib.controllers.statistics;

import edu.ap.gosmartlib.dto.statistics.BookPopularityDTO;
import edu.ap.gosmartlib.dto.statistics.ClassReadingStatsDTO;
import edu.ap.gosmartlib.dto.statistics.GenreStatsDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final AuthHelper authHelper;

    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    @GetMapping("/popular-books")
    public ResponseEntity<List<BookPopularityDTO>> popularBooks(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getMostPopularBooks(uid));
    }

    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    @GetMapping("/popular-genres")
    public ResponseEntity<List<GenreStatsDTO>> popularGenres(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getMostReadGenres(uid));
    }

    @PreAuthorize("hasAnyRole('TEACHER','BIBLIOTHEEKBEHEERDER','ADMIN')")
    @GetMapping("/highest-count-class")
    public ResponseEntity<List<ClassReadingStatsDTO>> highestCountClass(
            @AuthenticationPrincipal OAuth2User principal) {
        String uid = authHelper.extractUid(principal);
        return ResponseEntity.ok(statisticsService.getMostReadingClasses(uid));
    }
}
