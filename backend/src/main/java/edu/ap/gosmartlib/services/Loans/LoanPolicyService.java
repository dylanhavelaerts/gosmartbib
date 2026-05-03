package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.dto.loan.LoanPolicyDTO;
import edu.ap.gosmartlib.dto.loan.UpsertLoanPolicyRequest;
import edu.ap.gosmartlib.entities.LoanEntities.LoanPolicyEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
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

    public LoanPolicyDTO getPolicy(Long schoolId) {
        return loanPolicyRepository.findBySchool_Id(schoolId)
                .map(this::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Geen uitleen policy gevonden voor schoolId: " + schoolId));
    }

    public LoanPolicyDTO savePolicy(Long schoolId, UpsertLoanPolicyRequest request) {
        if (request.defaultLoanPeriodDays() < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "De uitleenperiode moet minimaal 1 dag zijn.");

        if (request.defaultExtensionPeriodDays() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "De verlengingsperiode kan niet negatief zijn.");

        LoanPolicyEntity policy = loanPolicyRepository.findBySchool_Id(schoolId)
                .orElseGet(() -> {
                    SchoolEntity school = schoolRepository.findById(schoolId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School met id " + schoolId + " niet gevonden."));

                    return new LoanPolicyEntity(school, 14, 3);
                });

        policy.setDefaultLoanPeriodDays(request.defaultLoanPeriodDays());
        policy.setDefaultExtensionPeriodDays(request.defaultExtensionPeriodDays());

        return toDTO(loanPolicyRepository.save(policy));
    }

    private LoanPolicyDTO toDTO(LoanPolicyEntity policy) {
        return new LoanPolicyDTO(
                policy.getSchool().getId(),
                policy.getDefaultLoanPeriodDays(),
                policy.getDefaultExtensionPeriodDays()
        );
    }
}
