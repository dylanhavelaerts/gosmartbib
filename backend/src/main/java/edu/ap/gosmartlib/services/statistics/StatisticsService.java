package edu.ap.gosmartlib.services.statistics;

import edu.ap.gosmartlib.dto.statistics.BookPopularityDTO;
import edu.ap.gosmartlib.dto.statistics.ClassReadingStatsDTO;
import edu.ap.gosmartlib.dto.statistics.GenreStatsDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final LoanHistoryRepository loanHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Haal de meest populaire boeken op basis van het aantal uitleningen
     * @param uid - nodig om per school te filteren
     * @return - lijst van populairste boeken (top 10)
     */
    public List<BookPopularityDTO> getMostPopularBooks(String uid) {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findMostPopularBooks(schoolId, PageRequest.of(0, 10))
                .stream()
                .map(row -> toBookPopularityDTO((BookEntity) row[0], (Long) row[1]))
                .toList();
    }

    /**
     * Haal de populairste genres op basis van het aantal uitleningen
     * @param uid - nodig om per school te filteren
     * @return - lijst van populairste genres (top 15)
     */
    public List<GenreStatsDTO> getMostReadGenres(String uid) {
        Long schoolId = resolveSchoolId(uid);
        return loanHistoryRepository.findMostReadGenres(schoolId, PageRequest.of(0, 15))
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
