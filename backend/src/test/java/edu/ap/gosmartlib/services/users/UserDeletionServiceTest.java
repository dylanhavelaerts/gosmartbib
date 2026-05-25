package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.*;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.bookRepositories.BookNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeletionServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private LoanHistoryRepository loanHistoryRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private ReadingListRepository readingListRepository;
    @Mock private BookNotificationRepository bookNotificationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserDeletionService userDeletionService;

    @Test
    void givenUser_whenDeleteUser_thenAnonymizesAllRelatedRecordsAndDeletesUser() {
        UserEntity user = new UserEntity();
        user.setSmartschoolUid("student-uid");

        userDeletionService.deleteUser(user);

        verify(loanRepository).anonymizeBySmartschoolUid("student-uid");
        verify(loanRepository).anonymizeExtensionDeciderBySmartschoolUid("student-uid");
        verify(loanHistoryRepository).anonymizeBySmartschoolUid("student-uid");
        verify(reviewRepository).anonymizeByUser(user);
        verify(purchaseRequestRepository).anonymizeByUser(user);
        verify(readingListRepository).deleteByCreator(user);
        verify(bookNotificationRepository).deleteByUser(user);
        verify(userRepository).delete(user);
        verifyNoMoreInteractions(loanRepository, loanHistoryRepository, reviewRepository,
                purchaseRequestRepository, readingListRepository, bookNotificationRepository,
                userRepository);
    }

    @Test
    void givenUser_whenDeleteUser_thenAnonymizesLoansBeforeDeletingUser() {
        UserEntity user = new UserEntity();
        user.setSmartschoolUid("teacher-uid");

        userDeletionService.deleteUser(user);

        var inOrder = inOrder(loanRepository, userRepository);
        inOrder.verify(loanRepository).anonymizeBySmartschoolUid("teacher-uid");
        inOrder.verify(userRepository).delete(user);
    }
}
