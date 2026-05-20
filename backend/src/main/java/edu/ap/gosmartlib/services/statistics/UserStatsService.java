package edu.ap.gosmartlib.services.statistics;

import edu.ap.gosmartlib.dto.statistics.PersonalReadingStatDTO;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final LoanHistoryRepository loanHistoryRepository;

    public PersonalReadingStatDTO getPersonalReadingStats(String uid) {
        int totalBooksRead = loanHistoryRepository
                .findBySmartschoolUserIdOrderByReturnDateDesc(uid).size();

        List<String> topGenres = loanHistoryRepository
                .findTopGenreForUser(uid, PageRequest.of(0, 3))
                .stream()
                .map(row -> (String) row[0])
                .toList();

        return new PersonalReadingStatDTO(totalBooksRead, topGenres);
    }
}
