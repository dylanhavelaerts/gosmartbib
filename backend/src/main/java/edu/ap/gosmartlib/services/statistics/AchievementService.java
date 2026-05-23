package edu.ap.gosmartlib.services.statistics;

import edu.ap.gosmartlib.dto.statistics.AchievementDTO;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private static final int[] BOOKS_THRESHOLDS   = {1, 5, 10, 25, 50, 100};
    private static final int[] PAGES_THRESHOLDS   = {1, 500, 1000, 5000, 10000, 25000};
    private static final int[] GENRES_THRESHOLDS  = {1, 5, 10, 15, 20};
    private static final int[] REVIEWS_THRESHOLDS = {1, 3, 5, 10, 25, 50};
    private static final int[] AUTHORS_THRESHOLDS = {1, 5, 15, 30, 40};

    private final LoanHistoryRepository loanHistoryRepository;
    private final ReviewRepository reviewRepository;

    @Cacheable("achievements")
    public List<AchievementDTO> getAchievements(String uid) {
        int booksRead      = loanHistoryRepository.findBySmartschoolUserId(uid).size();
        long pagesRead     = loanHistoryRepository.sumPagesByUid(uid);
        long genresRead    = loanHistoryRepository.countDistinctGenresByUid(uid);
        long reviewsWritten = reviewRepository.countByUser_SmartschoolUid(uid);
        long authorsRead   = loanHistoryRepository.countDistinctAuthorsByUid(uid);

        return List.of(
                compute("BOOKS_READ", "Boeken gelezen", booksRead, BOOKS_THRESHOLDS),
                compute("PAGES_READ", "Pagina's gelezen", (int) pagesRead, PAGES_THRESHOLDS),
                compute("GENRES_EXPLORED", "Genres ontdekt", (int) genresRead, GENRES_THRESHOLDS),
                compute("REVIEWS_WRITTEN", "Recensies geschreven", (int) reviewsWritten, REVIEWS_THRESHOLDS),
                compute("AUTHORS_READ", "Auteurs gelezen", (int) authorsRead, AUTHORS_THRESHOLDS)
        );
    }

    // Helper method om de huidige tier, 
    // volgende threshold en volgende tier te berekenen op basis van de waarde en drempels
    private AchievementDTO compute(String key, String label, int value, int[] thresholds) {
        int currentIndex = -1;
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (value >= thresholds[i]) {
                currentIndex = i;
                break;
            }
        }

        String currentTier  = currentIndex >= 0 ? tierAt(currentIndex, thresholds.length) : null;
        int nextThreshold   = (currentIndex < thresholds.length - 1) ? thresholds[currentIndex + 1] : 0;
        String nextTier     = (currentIndex < thresholds.length - 1) ? tierAt(currentIndex + 1, thresholds.length) : null;

        return new AchievementDTO(key, label, currentTier, value, nextThreshold, nextTier);
    }

    private String tierAt(int pos, int total) {
        if (pos == total - 1) return "LEGENDARY";
        String[] seq = {"BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND"};

        return seq[pos];
    }
}