package edu.ap.gosmartlib.services.loans;

import edu.ap.gosmartlib.dto.loan.LoanPolicyDTO;
import edu.ap.gosmartlib.dto.loan.UpsertLoanPolicyRequest;
import edu.ap.gosmartlib.entities.loan.LoanPolicyEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.repositories.loan.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanPolicyServiceTest {

    @Mock private LoanPolicyRepository loanPolicyRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private LoanPolicyService loanPolicyService;

    private static final Long SCHOOL_ID = 1L;
    private static final String UID = "student-uid";

    private SchoolEntity school;
    private LoanPolicyEntity policy;

    @BeforeEach
    void setUp() {
        school = new SchoolEntity();
        school.setId(SCHOOL_ID);

        policy = new LoanPolicyEntity(school, 14, 7);
        policy.setDueDateReminderDays(3);
    }

    @Test
    void givenExistingPolicy_whenGetPolicy_thenReturnsMappedDTO() {
        when(loanPolicyRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(policy));

        LoanPolicyDTO result = loanPolicyService.getPolicy(SCHOOL_ID);

        assertEquals(SCHOOL_ID, result.schoolId());
        assertEquals(14, result.defaultLoanPeriodDays());
        assertEquals(7, result.defaultExtensionPeriodDays());
        assertEquals(3, result.dueDateReminderDays());
    }

    @Test
    void givenNoPolicyForSchool_whenGetPolicy_thenThrows404() {
        when(loanPolicyRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> loanPolicyService.getPolicy(SCHOOL_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenUserWithPolicy_whenGetReminderDaysForUser_thenReturnsPolicyReminderDays() {
        UserEntity user = new UserEntity();
        user.setSmartschoolUid(UID);
        user.setSchool(school);

        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));
        when(loanPolicyRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(policy));

        int result = loanPolicyService.getReminderDaysForUser(UID);

        assertEquals(3, result);
    }

    @Test
    void givenUserNotFound_whenGetReminderDaysForUser_thenReturnsDefaultThree() {
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.empty());

        int result = loanPolicyService.getReminderDaysForUser(UID);

        assertEquals(3, result);
    }

    @Test
    void givenUserWithNoPolicyForSchool_whenGetReminderDaysForUser_thenReturnsDefaultThree() {
        UserEntity user = new UserEntity();
        user.setSmartschoolUid(UID);
        user.setSchool(school);

        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));
        when(loanPolicyRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());

        int result = loanPolicyService.getReminderDaysForUser(UID);

        assertEquals(3, result);
    }

    @Test
    void givenValidRequest_whenSavePolicy_thenUpdatesExistingPolicy() {
        UpsertLoanPolicyRequest request = new UpsertLoanPolicyRequest(21, 7, 5);
        when(loanPolicyRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.of(policy));
        when(loanPolicyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanPolicyDTO result = loanPolicyService.savePolicy(SCHOOL_ID, request);

        assertEquals(21, result.defaultLoanPeriodDays());
        assertEquals(7, result.defaultExtensionPeriodDays());
        assertEquals(5, result.dueDateReminderDays());
        verify(schoolRepository, never()).findById(any());
    }

    @Test
    void givenNoPolicyExists_whenSavePolicy_thenCreatesNewPolicyForSchool() {
        UpsertLoanPolicyRequest request = new UpsertLoanPolicyRequest(14, 3, 2);
        when(loanPolicyRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(loanPolicyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanPolicyDTO result = loanPolicyService.savePolicy(SCHOOL_ID, request);

        assertEquals(14, result.defaultLoanPeriodDays());
        assertEquals(3, result.defaultExtensionPeriodDays());
        assertEquals(2, result.dueDateReminderDays());

        ArgumentCaptor<LoanPolicyEntity> captor = ArgumentCaptor.forClass(LoanPolicyEntity.class);
        verify(loanPolicyRepository).save(captor.capture());
        assertEquals(school, captor.getValue().getSchool());
    }

    @Test
    void givenSchoolNotFound_whenSavePolicy_thenThrows404() {
        UpsertLoanPolicyRequest request = new UpsertLoanPolicyRequest(14, 3, 2);
        when(loanPolicyRepository.findBySchool_Id(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> loanPolicyService.savePolicy(SCHOOL_ID, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void givenLoanPeriodLessThanOne_whenSavePolicy_thenThrows400() {
        UpsertLoanPolicyRequest request = new UpsertLoanPolicyRequest(0, 7, 3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> loanPolicyService.savePolicy(SCHOOL_ID, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(loanPolicyRepository, schoolRepository);
    }

    @Test
    void givenNegativeExtensionPeriod_whenSavePolicy_thenThrows400() {
        UpsertLoanPolicyRequest request = new UpsertLoanPolicyRequest(14, -1, 3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> loanPolicyService.savePolicy(SCHOOL_ID, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(loanPolicyRepository, schoolRepository);
    }

    @Test
    void givenReminderDaysLessThanOne_whenSavePolicy_thenThrows400() {
        UpsertLoanPolicyRequest request = new UpsertLoanPolicyRequest(14, 7, 0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> loanPolicyService.savePolicy(SCHOOL_ID, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(loanPolicyRepository, schoolRepository);
    }
}
