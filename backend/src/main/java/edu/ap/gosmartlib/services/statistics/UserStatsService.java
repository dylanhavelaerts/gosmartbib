package edu.ap.gosmartlib.services.statistics;

import edu.ap.gosmartlib.dto.statistics.PersonalReadingStatDTO;
import edu.ap.gosmartlib.dto.statistics.ReaderProfileDTO;
import edu.ap.gosmartlib.entities.BookEntities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import edu.ap.gosmartlib.repositories.bookRepositories.BookRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private static final int MIN_BOOKS = 5;
    private static final int AVONTURIER_GENRES = 9;
    private static final double PIONIER_LOW_POP_RATIO = 0.5; // 50% van de boeken moet "low-popularity" zijn
    private static final int PIONIER_MAX_SCHOOL_LOANS = 5;
    private static final int SPRINTER_WINDOW_DAYS = 30;
    private static final int SPRINTER_BOOKS_IN_WINDOW = 4;
    private static final int TITAN_AVG_PAGES = 300;

    private final LoanHistoryRepository loanHistoryRepository;
    private final BookRepository bookRepository;

    public PersonalReadingStatDTO getPersonalReadingStats(String uid) {
        int totalBooksRead = (int) loanHistoryRepository.sumQuantityBySmartschoolUserId(uid);
        long totalPagesRead = loanHistoryRepository.sumPagesByUid(uid);

        List<String> topGenres = loanHistoryRepository
                .findTopGenreForUser(uid, PageRequest.of(0, 3))
                .stream()
                .map(row -> (String) row[0])
                .toList();

        return new PersonalReadingStatDTO(totalBooksRead, totalPagesRead, topGenres);
    }

    public ReaderProfileDTO getReaderProfile(String uid) {
        List<LoanHistoryEntity> history = loanHistoryRepository.findBySmartschoolUserId(uid);
        int booksRead = history.size();

        if (booksRead < MIN_BOOKS) {
            return new ReaderProfileDTO(null, null, booksRead, MIN_BOOKS - booksRead);
        }

        List<String> isbns = history.stream().map(LoanHistoryEntity::getIsbn).toList();
        Map<String, BookEntity> booksByIsbn = bookRepository.findByIsbnIn(isbns).stream()
                .collect(Collectors.toMap(BookEntity::getIsbn, b -> b));

        // scores worden berekend op basis van de criteria voor elk profiel (0 - 1)
        double avonturierScore = computeAvonturierScore(booksByIsbn);
        double pionierScore = computePionierScore(isbns, booksRead);
        double sprinterScore = computeSprinterScore(history);
        double titanScore = computeTitanScore(booksByIsbn);

        String winner = pickWinner(sprinterScore, pionierScore, titanScore, avonturierScore);
        String label = toLabel(winner);

        return new ReaderProfileDTO(winner, label, booksRead, 0);
    }

    @Cacheable("profileDistribution")
    public Map<String, Integer> getProfileDistribution(Long schoolId) {
        List<String> uids = loanHistoryRepository.findDistinctUserIdsBySchoolId(schoolId);

        Map<String, Integer> counts = new HashMap<>();
        counts.put("AVONTURIER", 0);
        counts.put("PIONIER", 0);
        counts.put("SPRINTER", 0);
        counts.put("TITAN", 0);

        int total = uids.size();
        if (total == 0) return counts;

        for (String uid : uids) {
            ReaderProfileDTO profile = getReaderProfile(uid);
            if (profile.profileType() != null) {
                counts.merge(profile.profileType(), 1, Integer::sum);
            }
        }

        Map<String, Integer> percentages = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            percentages.put(entry.getKey(), Math.round((entry.getValue() * 100f) / total));
        }
        return percentages;
    }

    private double computeAvonturierScore(Map<String, BookEntity> booksByIsbn) {
        long uniqueGenres = booksByIsbn.values().stream()
                .flatMap(b -> b.getCategories().stream())
                .distinct()
                .count();
        return Math.min(uniqueGenres / (double) AVONTURIER_GENRES, 1.0);
    }

    private double computePionierScore(List<String> isbns, int booksRead) {
        Map<String, Long> schoolWideCounts = loanHistoryRepository.countLoansByIsbnIn(isbns)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        long lowPopCount = isbns.stream()
                .distinct()
                .filter(isbn -> schoolWideCounts.getOrDefault(isbn, 0L) < PIONIER_MAX_SCHOOL_LOANS)
                .count();

        return Math.min(lowPopCount / (booksRead * PIONIER_LOW_POP_RATIO), 1.0);
    }

    private double computeSprinterScore(List<LoanHistoryEntity> history) {
        List<LocalDate> dates = history.stream()
                .map(LoanHistoryEntity::getReturnDate)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        int max = 0;
        for (int i = 0; i < dates.size(); i++) {
            LocalDate windowEnd = dates.get(i).plusDays(SPRINTER_WINDOW_DAYS);
            int count = 0;
            for (int j = i; j < dates.size() && !dates.get(j).isAfter(windowEnd); j++) {
                count++;
            }
            max = Math.max(max, count);
        }
        return Math.min(max / (double) SPRINTER_BOOKS_IN_WINDOW, 1.0);
    }

    private double computeTitanScore(Map<String, BookEntity> booksByIsbn) {
        OptionalDouble avg = booksByIsbn.values().stream()
                .filter(b -> b.getPageCount() != null && b.getPageCount() > 0)
                .mapToInt(BookEntity::getPageCount)
                .average();

        if (avg.isEmpty()) return 0.0;
        return Math.min(avg.getAsDouble() / TITAN_AVG_PAGES, 1.0);
    }

    private String pickWinner(double sprinter, double pionier, double titan, double avonturier) {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("PIONIER", pionier);
        scores.put("TITAN", titan);
        scores.put("SPRINTER", sprinter);
        scores.put("AVONTURIER", avonturier);

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("AVONTURIER");
    }

    private String toLabel(String profileType) {
        return switch (profileType) {
            case "AVONTURIER" -> "Avonturier";
            case "PIONIER" -> "Pionier";
            case "SPRINTER" -> "Sprinter";
            case "TITAN" -> "Titaan";
            default -> null;
        };
    }
}