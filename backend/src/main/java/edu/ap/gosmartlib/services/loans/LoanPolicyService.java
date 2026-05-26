package edu.ap.gosmartlib.services.loans;

import edu.ap.gosmartlib.dto.loan.LoanPolicyDTO;
import edu.ap.gosmartlib.dto.loan.UpsertLoanPolicyRequest;
import edu.ap.gosmartlib.entities.loanEntities.LoanPolicyEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
import edu.ap.gosmartlib.repositories.loanRepositories.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class LoanPolicyService {

    private final LoanPolicyRepository loanPolicyRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public LoanPolicyDTO getPolicy(Long schoolId) {
        return loanPolicyRepository.findBySchool_Id(schoolId)
                .map(this::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Geen uitleen policy gevonden voor schoolId: " + schoolId));
    }

    public int getReminderDaysForUser(String smartschoolUid) {
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .flatMap(user -> loanPolicyRepository.findBySchool_Id(user.getSchool().getId()))
                .map(LoanPolicyEntity::getDueDateReminderDays)
                .orElse(3);
    }

    public LoanPolicyDTO savePolicy(Long schoolId, UpsertLoanPolicyRequest request) {
        if (request.defaultLoanPeriodDays() < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "De uitleenperiode moet minimaal 1 dag zijn.");
        if (request.defaultExtensionPeriodDays() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "De verlengingsperiode kan niet negatief zijn.");
        if (request.dueDateReminderDays() < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "De herinneringsperiode moet minimaal 1 dag zijn.");

        LoanPolicyEntity policy = loanPolicyRepository.findBySchool_Id(schoolId)
                .orElseGet(() -> {
                    SchoolEntity school = schoolRepository.findById(schoolId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School met id " + schoolId + " niet gevonden."));
                    return new LoanPolicyEntity(school, 14, 3);
                });

        policy.setDefaultLoanPeriodDays(request.defaultLoanPeriodDays());
        policy.setDefaultExtensionPeriodDays(request.defaultExtensionPeriodDays());
        policy.setDueDateReminderDays(request.dueDateReminderDays());

        return toDTO(loanPolicyRepository.save(policy));
    }

    private LoanPolicyDTO toDTO(LoanPolicyEntity policy) {
        return new LoanPolicyDTO(
                policy.getSchool().getId(),
                policy.getDefaultLoanPeriodDays(),
                policy.getDefaultExtensionPeriodDays(),
                policy.getDueDateReminderDays()
        );
    }
}
