package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.SmartschoolMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDueDateNotificationServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private SmartschoolMessageService messageService;

    @InjectMocks
    private LoanDueDateNotificationService service;

    @Test
    void givenLoansExpiringInThreeDays_whenSendDueDateReminders_thenSendsMessageForEach() {
        LocalDate reminderDate = LocalDate.now().plusDays(3);
        LoanEntity loan1 = buildLoan("uid-1", "isbn-1", reminderDate);
        LoanEntity loan2 = buildLoan("uid-2", "isbn-2", reminderDate);
        UserEntity user1 = buildUser("uid-1");
        UserEntity user2 = buildUser("uid-2");
        BookEntity book1 = buildBook("isbn-1", "Clean Code");
        BookEntity book2 = buildBook("isbn-2", "The Pragmatic Programmer");

        when(loanRepository.findByDueDate(reminderDate)).thenReturn(List.of(loan1, loan2));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user1));
        when(userRepository.findBySmartschoolUid("uid-2")).thenReturn(Optional.of(user2));
        when(bookRepository.findByIsbn("isbn-1")).thenReturn(Optional.of(book1));
        when(bookRepository.findByIsbn("isbn-2")).thenReturn(Optional.of(book2));

        service.sendDueDateReminders();

        verify(messageService, times(1)).sendMessage(eq(user1), eq("Uitleentermijn bijna voorbij"), anyString());
        verify(messageService, times(1)).sendMessage(eq(user2), eq("Uitleentermijn bijna voorbij"), anyString());
    }

    @Test
    void givenNoLoansExpiringInThreeDays_whenSendDueDateReminders_thenSendsNoMessages() {
        when(loanRepository.findByDueDate(LocalDate.now().plusDays(3))).thenReturn(List.of());

        service.sendDueDateReminders();

        verify(messageService, never()).sendMessage(any(), any(), any());
    }

    @Test
    void givenUserNotFound_whenSendDueDateReminders_thenSkipsThatLoan() {
        LocalDate reminderDate = LocalDate.now().plusDays(3);
        LoanEntity loan = buildLoan("uid-unknown", "isbn-1", reminderDate);

        when(loanRepository.findByDueDate(reminderDate)).thenReturn(List.of(loan));
        when(userRepository.findBySmartschoolUid("uid-unknown")).thenReturn(Optional.empty());

        service.sendDueDateReminders();

        verify(messageService, never()).sendMessage(any(), any(), any());
    }

    @Test
    void givenBookNotFound_whenSendDueDateReminders_thenSkipsThatLoan() {
        LocalDate reminderDate = LocalDate.now().plusDays(3);
        LoanEntity loan = buildLoan("uid-1", "isbn-unknown", reminderDate);
        UserEntity user = buildUser("uid-1");

        when(loanRepository.findByDueDate(reminderDate)).thenReturn(List.of(loan));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookRepository.findByIsbn("isbn-unknown")).thenReturn(Optional.empty());

        service.sendDueDateReminders();

        verify(messageService, never()).sendMessage(any(), any(), any());
    }

    // --- helpers ---

    private LoanEntity buildLoan(String userId, String isbn, LocalDate dueDate) {
        LoanEntity loan = new LoanEntity();
        loan.setSmartschoolUserId(userId);
        loan.setIsbn(isbn);
        loan.setLoanDate(dueDate.minusDays(14));
        loan.setDueDate(dueDate);
        loan.setQuantity(1);
        return loan;
    }

    private UserEntity buildUser(String uid) {
        UserEntity user = new UserEntity();
        user.setSmartschoolUid(uid);
        return user;
    }

    private BookEntity buildBook(String isbn, String title) {
        BookEntity book = new BookEntity();
        book.setIsbn(isbn);
        book.setTitle(title);
        return book;
    }
}
