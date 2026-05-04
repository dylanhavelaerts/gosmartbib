package edu.ap.gosmartlib.dto.loan;

import edu.ap.gosmartlib.util.UserRoles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LoanExtensionRequestDTO(
                Long loanId,
                String smartschoolUserId,
                String borrowerDisplayName,
                UserRoles borrowerRole,
                int quantity,
                LocalDate loanDate,
                LocalDate currentDueDate,
                LocalDate proposedDueDate,
                LocalDateTime requestedAt,
                LoanBookDTO book) {
        public record LoanBookDTO(
                        Long id,
                        String title,
                        String thumbnail,
                        String isbn,
                        List<String> authors) {
        }
}