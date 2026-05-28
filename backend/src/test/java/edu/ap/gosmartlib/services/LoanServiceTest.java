package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO;
import edu.ap.gosmartlib.dto.loan.LoanExtensionRequestDTO;
import edu.ap.gosmartlib.dto.userdirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.dto.userdirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.book.BookInventoryEntity;
import edu.ap.gosmartlib.entities.loan.LoanExtensionStatus;
import edu.ap.gosmartlib.entities.*;
import edu.ap.gosmartlib.entities.loan.LoanEntity;
import edu.ap.gosmartlib.entities.loan.LoanPolicyEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.repositories.*;
import edu.ap.gosmartlib.repositories.loan.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.loan.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.loan.LoanRepository;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.services.loans.LoanService;
import edu.ap.gosmartlib.services.messages.BookNotificationService;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.util.UserRoles;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private UserDirectoryService userDirectoryService;

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
        LoanRequestDTO request = new LoanRequestDTO(1L, 2,
                new SmartschoolUserDTO("uid-1", null, null, null, null, null));

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
        LoanRequestDTO request = new LoanRequestDTO(1L, 2,
                new SmartschoolUserDTO("uid-1", null, null, null, null, null));

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

    private UserEntity buildUserWithRole(Long id, String uid, SchoolEntity school, UserRoles role) {
        UserEntity user = buildUser(id, uid, school);
        user.setRole(role);
        return user;
    }

    // --- createLoans ---
    @Test
    void givenValidRequest_whenCreateLoans_thenSavesLoanAndUpdatesInventory() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 5); // 5 beschikbaar
        BookEntity book = buildBookWithInventory("9780000000001", 5, inventory);
        book.setId(10L);
        UserEntity borrower = buildUser(1L, "uid-1", school);

        // Mock de DTOs zodat we de getters kunnen nabootsen
        LoanRequestDTO request = mock(LoanRequestDTO.class);
        // We gaan er even vanuit dat je request een user record of class heeft:
        edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO userDto = mock(
                edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO.class);

        when(request.bookId()).thenReturn(10L);
        when(request.quantity()).thenReturn(2);
        when(request.user()).thenReturn(userDto);
        when(userDto.smartschoolUserId()).thenReturn("uid-1");

        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        loanService.createLoans(List.of(request));

        // Assert: Controleer of de voorraad is verminderd
        assertEquals(3, book.getAvailableCopies());
        assertEquals(3, inventory.getAvailableCopies());

        // Assert: Controleer of loan en boek zijn opgeslagen
        verify(bookRepository, times(1)).save(book);
        verify(loanRepository, times(1)).save(any(LoanEntity.class));
    }

    @Test
    void givenNotEnoughInventory_whenCreateLoans_thenThrowsIllegalArgumentException() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        BookInventoryEntity inventory = buildInventory(school, 1); // Slechts 1 beschikbaar
        BookEntity book = buildBookWithInventory("9780000000001", 1, inventory);
        book.setId(10L);
        UserEntity borrower = buildUser(1L, "uid-1", school);

        LoanRequestDTO request = mock(LoanRequestDTO.class);
        edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO userDto = mock(
                edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO.class);

        when(request.bookId()).thenReturn(10L);
        when(request.quantity()).thenReturn(2); // Je wilt er 2 lenen
        when(request.user()).thenReturn(userDto);
        when(userDto.smartschoolUserId()).thenReturn("uid-1");

        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoans(List.of(request)));

        assertTrue(ex.getMessage().contains("Niet genoeg exemplaren beschikbaar"));
        verify(loanRepository, never()).save(any(LoanEntity.class));
    }

    // --- getActiveLoansByUser ---
    @Test
    void givenActiveLoans_whenGetActiveLoansByUser_thenReturnsMappedDTOs() {
        // Arrange
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 2);
        BookEntity book = buildBookWithInventory("9780000000001", 5, buildInventory(buildSchool(1L), 5));
        book.setId(10L);
        book.setTitle("Test Book");

        when(loanRepository.findBySmartschoolUserId("uid-1")).thenReturn(List.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));

        // Act
        var results = loanService.getActiveLoansByUser("uid-1");

        // Assert
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).loanId());
        assertEquals("uid-1", results.get(0).smartschoolUserId());
        assertEquals(2, results.get(0).quantity());
        assertEquals("NONE", results.get(0).extensionStatus());
        assertNotNull(results.get(0).book());
        assertEquals("Test Book", results.get(0).book().title());
    }
// --- getActiveLoansAsAdmin ---

    @Test
    void givenBeheerder_whenGetActiveLoansAsAdmin_thenReturnsMappedDTOs() {
        SchoolEntity school = buildSchool(5L);
        UserEntity beheerder = buildUserWithRole(1L, "beheerder-1", school, UserRoles.LIBRARIAN);

        LoanEntity loan = buildLoan(1L, "lener-1", "9780000000001", 2);
        BookEntity book = buildBookWithInventory("9780000000001", 5, buildInventory(school, 5));
        book.setId(10L);
        book.setTitle("Test Book");

        when(userRepository.findBySmartschoolUid("beheerder-1")).thenReturn(Optional.of(beheerder));
        when(loanRepository.findBySmartschoolUserId("lener-1")).thenReturn(List.of(loan));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));

        var results = loanService.getActiveLoansAsAdmin("beheerder-1", "lener-1");

        assertEquals(1, results.size());
        assertEquals("lener-1", results.get(0).smartschoolUserId());
    }

    @Test
    void givenNonBeheerder_whenGetActiveLoansAsAdmin_thenThrowsException() {
        SchoolEntity school = buildSchool(5L);
        UserEntity student = buildUserWithRole(1L, "uid-1", school, UserRoles.STUDENT);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
                () -> loanService.getActiveLoansAsAdmin("uid-1", "lener-2"));

        verify(loanRepository, never()).findBySmartschoolUserId(any());
    }

    // --- verlengingsaanvragen ---

    @Test
    void givenStudentOwnLoan_whenRequestLoanExtension_thenSetsStatusPending() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity student = buildUserWithRole(1L, "uid-1", school, UserRoles.STUDENT);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.NONE);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(student));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act
        loanService.requestLoanExtension(1L, "uid-1");

        // Assert
        assertEquals(LoanExtensionStatus.PENDING, loan.getExtensionStatus());
        assertNotNull(loan.getExtensionRequestedAt());
        assertNull(loan.getExtensionDecidedAt());
        assertNull(loan.getExtensionDecidedBySmartschoolUserId());

        verify(loanRepository, times(1)).save(loan);
    }

    @Test
    void givenTeacherOwnLoan_whenRequestLoanExtension_thenSetsStatusPending() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity teacher = buildUserWithRole(1L, "teacher-1", school, UserRoles.TEACHER);
        LoanEntity loan = buildLoan(1L, "teacher-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.NONE);

        when(userRepository.findBySmartschoolUid("teacher-1")).thenReturn(Optional.of(teacher));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act
        loanService.requestLoanExtension(1L, "teacher-1");

        // Assert
        assertEquals(LoanExtensionStatus.PENDING, loan.getExtensionStatus());
        assertNotNull(loan.getExtensionRequestedAt());

        verify(loanRepository, times(1)).save(loan);
    }

    @Test
    void givenBibliotheekbeheerder_whenRequestLoanExtension_thenThrowsException() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity beheerder = buildUserWithRole(1L, "beheerder-1", school, UserRoles.LIBRARIAN);

        when(userRepository.findBySmartschoolUid("beheerder-1")).thenReturn(Optional.of(beheerder));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.requestLoanExtension(1L, "beheerder-1"));

        assertTrue(ex.getMessage().contains("Alleen leerlingen en leerkrachten"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenLoanFromOtherUser_whenRequestLoanExtension_thenThrowsException() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity student = buildUserWithRole(1L, "uid-1", school, UserRoles.STUDENT);
        LoanEntity loan = buildLoan(1L, "other-uid", "9780000000001", 1);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(student));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.requestLoanExtension(1L, "uid-1"));

        assertTrue(ex.getMessage().contains("alleen een verlenging aanvragen voor je eigen uitleningen"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenPendingLoan_whenRequestLoanExtension_thenThrowsException() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity student = buildUserWithRole(1L, "uid-1", school, UserRoles.STUDENT);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.PENDING);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(student));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.requestLoanExtension(1L, "uid-1"));

        assertTrue(ex.getMessage().contains("Er staat al een verlengingsaanvraag open"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenApprovedLoan_whenRequestLoanExtension_thenThrowsException() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity student = buildUserWithRole(1L, "uid-1", school, UserRoles.STUDENT);
        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.APPROVED);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(student));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.requestLoanExtension(1L, "uid-1"));

        assertTrue(ex.getMessage().contains("Deze lening werd al verlengd"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenBibliotheekbeheerder_whenGetPendingExtensionRequestsForSchool_thenReturnsOnlyOwnSchoolRequests() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity beheerder = buildUserWithRole(1L, "beheerder-1", school, UserRoles.LIBRARIAN);
        UserEntity borrower = buildUserWithRole(2L, "uid-1", school, UserRoles.STUDENT);

        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.PENDING);
        loan.setExtensionRequestedAt(LocalDateTime.now().minusDays(1));

        BookEntity book = buildBookWithInventory("9780000000001", 5, buildInventory(school, 5));
        book.setId(10L);
        book.setTitle("Test Book");
        book.setThumbnail("cover.jpg");

        when(userRepository.findBySmartschoolUid("beheerder-1")).thenReturn(Optional.of(beheerder));
        when(loanRepository.findExtensionRequestsForSchool(LoanExtensionStatus.PENDING, 5L))
                .thenReturn(List.of(loan));

        when(userDirectoryService.resolveDisplayNames(
                eq("beheerder-1"),
                any(ResolveDisplayNamesRequest.class)))
                .thenReturn(new ResolveDisplayNamesResponse(
                        true,
                        1,
                        1,
                        Map.of("uid-1", "Jan Janssens"),
                        List.of(),
                        "OK"));

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));

        // Act
        List<LoanExtensionRequestDTO> results = loanService.getPendingExtensionRequestsForSchool("beheerder-1");

        // Assert
        assertEquals(1, results.size());

        LoanExtensionRequestDTO dto = results.get(0);
        assertEquals(1L, dto.loanId());
        assertEquals("uid-1", dto.smartschoolUserId());
        assertEquals("Jan Janssens", dto.borrowerDisplayName());
        assertEquals(UserRoles.STUDENT, dto.borrowerRole());
        assertEquals(loan.getDueDate(), dto.currentDueDate());
        assertEquals(loan.getDueDate().plusDays(21), dto.proposedDueDate());
        assertNotNull(dto.requestedAt());
        assertNotNull(dto.book());
        assertEquals("Test Book", dto.book().title());

        verify(userDirectoryService, times(1)).resolveDisplayNames(
                eq("beheerder-1"),
                any(ResolveDisplayNamesRequest.class));
    }

    @Test
    void givenPendingLoanFromOwnSchool_whenApproveLoanExtension_thenDoublesLoanPeriod() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity beheerder = buildUserWithRole(1L, "beheerder-1", school, UserRoles.LIBRARIAN);
        UserEntity borrower = buildUserWithRole(2L, "uid-1", school, UserRoles.STUDENT);

        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setLoanDate(LocalDate.now().minusDays(7));
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setExtensionStatus(LoanExtensionStatus.PENDING);

        LocalDate originalDueDate = loan.getDueDate();

        when(userRepository.findBySmartschoolUid("beheerder-1")).thenReturn(Optional.of(beheerder));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act
        loanService.approveLoanExtension(1L, "beheerder-1");

        // Assert
        assertEquals(originalDueDate.plusDays(21), loan.getDueDate());
        assertEquals(LoanExtensionStatus.APPROVED, loan.getExtensionStatus());
        assertNotNull(loan.getExtensionDecidedAt());
        assertEquals("beheerder-1", loan.getExtensionDecidedBySmartschoolUserId());

        verify(loanRepository, times(1)).save(loan);
    }

    @Test
    void givenPendingLoanFromOwnSchool_whenDenyLoanExtension_thenSetsStatusDenied() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity beheerder = buildUserWithRole(1L, "beheerder-1", school, UserRoles.LIBRARIAN);
        UserEntity borrower = buildUserWithRole(2L, "uid-1", school, UserRoles.STUDENT);

        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.PENDING);

        when(userRepository.findBySmartschoolUid("beheerder-1")).thenReturn(Optional.of(beheerder));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act
        loanService.denyLoanExtension(1L, "beheerder-1");

        // Assert
        assertEquals(LoanExtensionStatus.DENIED, loan.getExtensionStatus());
        assertNotNull(loan.getExtensionDecidedAt());
        assertEquals("beheerder-1", loan.getExtensionDecidedBySmartschoolUserId());

        verify(loanRepository, times(1)).save(loan);
    }

    @Test
    void givenPendingLoanFromOtherSchool_whenApproveLoanExtension_thenThrowsException() {
        // Arrange
        SchoolEntity beheerderSchool = buildSchool(5L);
        SchoolEntity otherSchool = buildSchool(99L);

        UserEntity beheerder = buildUserWithRole(1L, "beheerder-1", beheerderSchool, UserRoles.LIBRARIAN);
        UserEntity borrower = buildUserWithRole(2L, "uid-1", otherSchool, UserRoles.STUDENT);

        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.PENDING);

        when(userRepository.findBySmartschoolUid("beheerder-1")).thenReturn(Optional.of(beheerder));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.approveLoanExtension(1L, "beheerder-1"));

        assertTrue(ex.getMessage().contains("eigen school"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenLoanWithoutPendingRequest_whenApproveLoanExtension_thenThrowsException() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity beheerder = buildUserWithRole(1L, "beheerder-1", school, UserRoles.LIBRARIAN);
        UserEntity borrower = buildUserWithRole(2L, "uid-1", school, UserRoles.STUDENT);

        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.NONE);

        when(userRepository.findBySmartschoolUid("beheerder-1")).thenReturn(Optional.of(beheerder));
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(borrower));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.approveLoanExtension(1L, "beheerder-1"));

        assertTrue(ex.getMessage().contains("geen open verlengingsaanvraag"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenDeniedLoan_whenRequestLoanExtension_thenThrowsException() {
        // Arrange
        SchoolEntity school = buildSchool(5L);
        UserEntity student = buildUserWithRole(1L, "uid-1", school, UserRoles.STUDENT);

        LoanEntity loan = buildLoan(1L, "uid-1", "9780000000001", 1);
        loan.setExtensionStatus(LoanExtensionStatus.DENIED);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(student));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.requestLoanExtension(1L, "uid-1"));

        assertTrue(ex.getMessage().contains("werd al geweigerd"));
        verify(loanRepository, never()).save(any());
    }
}
