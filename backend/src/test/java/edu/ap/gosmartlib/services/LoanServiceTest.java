package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO;
import edu.ap.gosmartlib.entities.*;
import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanPolicyEntity;
import edu.ap.gosmartlib.repositories.*;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.services.Loans.LoanService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanHistoryRepository loanHistoryRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookNotificationService bookNotificationService;
    @Mock
    private LoanPolicyRepository loanPolicyRepository;

    @InjectMocks
    private LoanService loanService;

    // --- returnBook ---

    @Test
    void givenValidLoan_whenReturnBook_thenIncreasesGlobalAndSchoolInventory() {
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 1);
        BookEntity book = buildBookWithInventory("9780000000001", 3, inventory);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 2);
        UserEntity borrower = buildUser(1L, "uid-1", school);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));

        loanService.returnBook(1L, 2);

        assertEquals(5, book.getAvailableCopies());
        assertEquals(3, inventory.getAvailableCopies());
    }

    @Test
    void givenSchoolInventoryWasZero_whenReturnBook_thenTriggersNotifications() {
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 0);
        BookEntity book = buildBookWithInventory("9780000000001", 0, inventory);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        UserEntity borrower = buildUser(1L, "uid-1", school);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));

        loanService.returnBook(1L, 1);

        verify(bookNotificationService, times(1)).triggerNotificationsForBook(book, 5L);
    }

    @Test
    void givenSchoolInventoryAboveZero_whenReturnBook_thenDoesNotTriggerNotifications() {
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 2);
        BookEntity book = buildBookWithInventory("9780000000001", 3, inventory);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        UserEntity borrower = buildUser(1L, "uid-1", school);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));

        loanService.returnBook(1L, 1);

        verify(bookNotificationService, never()).triggerNotificationsForBook(any(), any());
    }

    @Test
    void givenLoanNotFound_whenReturnBook_thenThrowsException() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> loanService.returnBook(99L, 1));
    }

    @Test
    void givenQuantityExceedsLoan_whenReturnBook_thenThrowsException() {
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 2);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThrows(IllegalArgumentException.class, () -> loanService.returnBook(1L, 5));
    }

    @Test
    void givenQuantityZero_whenReturnBook_thenThrowsException() {
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 2);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThrows(IllegalArgumentException.class, () -> loanService.returnBook(1L, 0));
    }

    @Test
    void givenFullReturn_whenReturnBook_thenDeletesLoanRecord() {
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 1);
        BookEntity book = buildBookWithInventory("9780000000001", 2, inventory);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 2);
        UserEntity borrower = buildUser(1L, "uid-1", school);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));

        loanService.returnBook(1L, 2);

        verify(loanRepository, times(1)).delete(loan);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenPartialReturn_whenReturnBook_thenUpdatesLoanQuantity() {
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 1);
        BookEntity book = buildBookWithInventory("9780000000001", 3, inventory);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 3);
        UserEntity borrower = buildUser(1L, "uid-1", school);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));

        loanService.returnBook(1L, 1);

        assertEquals(2, loan.getQuantity());
        verify(loanRepository, times(1)).save(loan);
        verify(loanRepository, never()).delete(any());
    }

    @Test
    void givenBorrowerNotFound_whenReturnBook_thenStillIncreasesGlobalInventoryWithoutNotifying() {
        BookInventoryEntity inventory = buildInventory(buildSchool(5L), 0);
        BookEntity book = buildBookWithInventory("9780000000001", 0, inventory);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.empty());

        loanService.returnBook(1L, 1);

        assertEquals(1, book.getAvailableCopies());
        verify(bookNotificationService, never()).triggerNotificationsForBook(any(), any());
    }

    // --- createLoans ---

    @Test
    void givenPolicyExists_whenCreateLoans_thenUsesPolicyLoanPeriod() {
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 3);
        BookEntity book = buildBookWithInventory("9780000000001", 5, inventory);
        UserEntity borrower = buildUser(1L, "uid-1", school);
        LoanPolicyEntity policy = new LoanPolicyEntity(school, 21, 7);
        LoanRequestDTO request = new LoanRequestDTO(1L, 2, new SmartschoolUserDTO("uid-1", null, null, null, null, null));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(loanPolicyRepository.findBySchool_Id(5L)).thenReturn(Optional.of(policy));

        loanService.createLoans(List.of(request));

        ArgumentCaptor<LoanEntity> captor = ArgumentCaptor.forClass(LoanEntity.class);
        verify(loanRepository).save(captor.capture());
        assertEquals(LocalDate.now().plusDays(21), captor.getValue().getDueDate());
    }

    @Test
    void givenNoPolicyExists_whenCreateLoans_thenUsesDefaultFourteenDays() {
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 3);
        BookEntity book = buildBookWithInventory("9780000000001", 5, inventory);
        UserEntity borrower = buildUser(1L, "uid-1", school);
        LoanRequestDTO request = new LoanRequestDTO(1L, 2, new SmartschoolUserDTO("uid-1", null, null, null, null, null));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(loanPolicyRepository.findBySchool_Id(5L)).thenReturn(Optional.empty());

        loanService.createLoans(List.of(request));

        ArgumentCaptor<LoanEntity> captor = ArgumentCaptor.forClass(LoanEntity.class);
        verify(loanRepository).save(captor.capture());
        assertEquals(LocalDate.now().plusDays(14), captor.getValue().getDueDate());
    }

    // --- helpers ---

    private LoanEntity buildLoan(Long id, String userId, String isbn, int quantity) {
        LoanEntity loan = new LoanEntity();
        loan.setId(id);
        loan.setSmartschoolUserId(userId);
        loan.setIsbn(isbn);
        loan.setQuantity(quantity);
        loan.setLoanDate(LocalDate.now().minusDays(7));
        loan.setDueDate(LocalDate.now().plusDays(14));
        return loan;
    }

    private BookEntity buildBookWithInventory(String isbn, int availableCopies, BookInventoryEntity inventory) {
        BookEntity book = new BookEntity();
        book.setIsbn(isbn);
        book.setAvailableCopies(availableCopies);
        book.getInventories().add(inventory);
        return book;
    }

    private BookInventoryEntity buildInventory(SchoolEntity school, int availableCopies) {
        BookInventoryEntity inv = new BookInventoryEntity();
        inv.setSchool(school);
        inv.setAvailableCopies(availableCopies);
        return inv;
    }

    private SchoolEntity buildSchool(Long id) {
        SchoolEntity school = new SchoolEntity();
        school.setId(id);
        return school;
    }

    private UserEntity buildUser(Long id, String uid, SchoolEntity school) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(uid);
        user.setSchool(school);
        return user;
    }
}
