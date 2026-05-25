package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.*;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.bookRepositories.BookNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final LoanRepository loanRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ReadingListRepository readingListRepository;
    private final BookNotificationRepository bookNotificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void deleteUser(UserEntity user) {
        String uid = user.getSmartschoolUid();
        log.info("Deleting user {} and anonymizing related records", uid);

        loanRepository.anonymizeBySmartschoolUid(uid);
        loanRepository.anonymizeExtensionDeciderBySmartschoolUid(uid);
        loanHistoryRepository.anonymizeBySmartschoolUid(uid);
        reviewRepository.anonymizeByUser(user);
        purchaseRequestRepository.anonymizeByUser(user);
        readingListRepository.deleteByCreator(user);
        bookNotificationRepository.deleteByUser(user);

        userRepository.delete(user);
        log.info("User {} deleted", uid);
    }
}
