package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.dto.loan.AdminActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.AdminLoanHistoryDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.bookRepositories.BookRepository;
import edu.ap.gosmartlib.repositories.loanRepositories.LoanHistoryRepository;
import edu.ap.gosmartlib.repositories.loanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private LoanHistoryRepository loanHistoryRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserDirectoryService userDirectoryService;
    @Mock private AdminLoanMapper mapper;
    @Mock private LoanAccessGuard accessGuard;
    @Mock private SchoolClassRepository schoolClassRepository;

    @InjectMocks private AdminLoanService adminLoanService;

    private UserEntity mockActor(Long schoolId) {
        UserEntity actor = mock(UserEntity.class);
        SchoolEntity school = mock(SchoolEntity.class);
        when(school.getId()).thenReturn(schoolId);
        when(actor.getSchool()).thenReturn(school);
        return actor;
    }

    // --- getActiveLoansForSchool ---

    @Test
    void givenNoLoans_whenGetActiveLoansForSchool_thenReturnsEmptyPage() {
        UserEntity actor = mockActor(1L);
        when(accessGuard.requireBibliotheekbeheerder("uid")).thenReturn(actor);
        when(loanRepository.findAllActiveBySchoolId(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<AdminActiveLoanDTO> result = adminLoanService.getActiveLoansForSchool("uid", null, 0, 10);

        assertTrue(result.isEmpty());
        verify(mapper, never()).toActiveDTO(any(), any(), any(), any());
    }

    @Test
    void givenLoansExist_whenGetActiveLoansForSchool_thenReturnsMappedPage() {
        UserEntity actor = mockActor(1L);
        when(accessGuard.requireBibliotheekbeheerder("uid")).thenReturn(actor);

        var loan = mock(edu.ap.gosmartlib.entities.loanEntities.LoanEntity.class);
        when(loan.getSmartschoolUserId()).thenReturn("student1");
        when(loan.getIsbn()).thenReturn("isbn1");

        Page<edu.ap.gosmartlib.entities.loanEntities.LoanEntity> loanPage =
                new PageImpl<>(List.of(loan), PageRequest.of(0, 10), 1);
        when(loanRepository.findAllActiveBySchoolId(eq(1L), any(Pageable.class))).thenReturn(loanPage);
        when(bookRepository.findByIsbnIn(any())).thenReturn(List.of());
        when(userRepository.findAllBySchool_IdAndSmartschoolUidIn(any(), any())).thenReturn(List.of());

        AdminActiveLoanDTO dto = mock(AdminActiveLoanDTO.class);
        when(mapper.toActiveDTO(any(), any(), any(), any())).thenReturn(dto);

        Page<AdminActiveLoanDTO> result = adminLoanService.getActiveLoansForSchool("uid", null, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(dto, result.getContent().get(0));
    }

    @Test
    void givenClassId_whenGetActiveLoansForSchool_thenUsesClassFilter() {
        UserEntity actor = mockActor(1L);
        when(accessGuard.requireBibliotheekbeheerder("uid")).thenReturn(actor);
        when(loanRepository.findAllActiveBySchoolIdAndClassId(eq(1L), eq(5L), any(Pageable.class)))
                .thenReturn(Page.empty());

        adminLoanService.getActiveLoansForSchool("uid", 5L, 0, 10);

        verify(loanRepository).findAllActiveBySchoolIdAndClassId(eq(1L), eq(5L), any(Pageable.class));
        verify(loanRepository, never()).findAllActiveBySchoolId(any(), any());
    }

    // --- getLoanHistoryForSchool ---

    @Test
    void givenNoHistory_whenGetLoanHistoryForSchool_thenReturnsEmptyPage() {
        UserEntity actor = mockActor(1L);
        when(accessGuard.requireBibliotheekbeheerder("uid")).thenReturn(actor);
        when(loanHistoryRepository.findAllBySchoolIdOrderByReturnDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<AdminLoanHistoryDTO> result = adminLoanService.getLoanHistoryForSchool("uid", null, 0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void givenClassId_whenGetLoanHistoryForSchool_thenUsesClassFilter() {
        UserEntity actor = mockActor(1L);
        when(accessGuard.requireBibliotheekbeheerder("uid")).thenReturn(actor);
        when(loanHistoryRepository.findAllBySchoolIdAndClassIdOrderByReturnDateDesc(eq(1L), eq(3L), any(Pageable.class)))
                .thenReturn(Page.empty());

        adminLoanService.getLoanHistoryForSchool("uid", 3L, 0, 10);

        verify(loanHistoryRepository).findAllBySchoolIdAndClassIdOrderByReturnDateDesc(eq(1L), eq(3L), any(Pageable.class));
        verify(loanHistoryRepository, never()).findAllBySchoolIdOrderByReturnDateDesc(any(), any());
    }

    // --- getSchoolClasses ---

    @Test
    void givenSchoolWithClasses_whenGetSchoolClasses_thenReturnsMappedList() {
        UserEntity actor = mockActor(1L);
        when(accessGuard.requireBibliotheekbeheerder("uid")).thenReturn(actor);

        SchoolClassEntity cls = mock(SchoolClassEntity.class);
        when(cls.getId()).thenReturn(10L);
        when(cls.getName()).thenReturn("3A");
        when(schoolClassRepository.findAllBySchool_IdOrderByNameAsc(1L)).thenReturn(List.of(cls));

        List<ReadingListAssignmentTargetsDTO.ClassTarget> result =
                adminLoanService.getSchoolClasses("uid");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals("3A", result.get(0).name());
    }
}
