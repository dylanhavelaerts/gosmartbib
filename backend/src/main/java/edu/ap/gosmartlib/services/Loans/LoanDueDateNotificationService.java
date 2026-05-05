package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.LoanRepositories.LoanRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.SmartschoolMessageService;
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
    private final SmartschoolMessageService messageService;

    @Scheduled(cron = "0 0 8 * * *") 
    @Transactional(readOnly = true)
    public void sendDueDateReminders() {
        LocalDate reminderDate = LocalDate.now().plusDays(3);
        List<LoanEntity> loansAboutToExpire = loanRepository.findByDueDate(reminderDate);

        log.info("Uitleentermijn-herinneringen: {} leningen verlopen op {}", loansAboutToExpire.size(), reminderDate);

        for (LoanEntity loan : loansAboutToExpire) {
            userRepository.findBySmartschoolUid(loan.getSmartschoolUserId()).ifPresent(user ->
                    bookRepository.findByIsbn(loan.getIsbn()).ifPresent(book ->
                            messageService.sendMessage(
                                    user,
                                    "Uitleentermijn bijna voorbij",
                                    "Let op: Dit betreft een geautomatiseerd bericht; reacties worden niet in behandeling genomen. GoSmartBib vraagt u nooit om op links te klikken via een e-mail.\n" +
                                            "Het boek '" + book.getTitle() + "' moet uiterlijk op " + loan.getDueDate() + " worden teruggebracht."
                            )
                    )
            );
        }
    }
}
