package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.entities.loanEntities.LoanEntity;
import edu.ap.gosmartlib.repositories.bookRepositories.BookRepository;
import edu.ap.gosmartlib.repositories.loanRepositories.LoanPolicyRepository;
import edu.ap.gosmartlib.repositories.loanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.messages.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanDueDateNotificationService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final MessageSender messageService;
    private final LoanPolicyRepository loanPolicyRepository;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendDueDateReminders() {
        LocalDate today = LocalDate.now();

        loanPolicyRepository.findAll().forEach(policy -> {
            LocalDate reminderDate = today.plusDays(policy.getDueDateReminderDays());
            List<LoanEntity> loans = loanRepository.findByDueDate(reminderDate);

            log.info("School {}: {} leningen verlopen op {}", policy.getSchool().getId(), loans.size(), reminderDate);

            for (LoanEntity loan : loans) {
                userRepository.findBySmartschoolUid(loan.getSmartschoolUserId()).ifPresent(user -> {
                    if (user.getSchool().getId().equals(policy.getSchool().getId())) {
                        bookRepository.findByIsbn(loan.getIsbn()).ifPresent(book ->
                                messageService.sendMessage(
                                        user,
                                        "Uitleentermijn bijna voorbij",
                                        "Let op: Dit betreft een geautomatiseerd bericht; reacties worden niet in behandeling genomen. GoSmartBib vraagt u nooit om op links te klikken via een e-mail.\n" +
                                                "Het boek '" + book.getTitle() + "' moet uiterlijk op " + loan.getDueDate() + " worden teruggebracht."
                                )
                        );
                    }
                });
            }
        });
    }
}
