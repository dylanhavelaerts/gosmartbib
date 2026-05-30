package edu.ap.gosmartlib.services.messages;

import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.book.BookNotificationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.book.BookNotificationRepository;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Beheert beschikbaarheidsmeldingen voor boeken.
 * Gebruikers kunnen zich abonneren op een enkel boek of een volledige leeslijst (bulk).
 * Abonnementen zijn eenmalig: na verzending via triggerNotificationsForBook worden ze verwijderd,
 * ook als het versturen mislukt. Gemiste meldingen worden niet herhaald.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookNotificationService {

    private final BookNotificationRepository bookNotificationRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final MessageSender messageService;

    /**
     * Abonneert de gebruiker op een beschikbaarheidsmelding voor het opgegeven boek.
     * Dubbele abonnementen worden genegeerd.
     */
    @Transactional
    public void enable(String smartschoolUid, Long bookId) {
        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boek niet gevonden"));

        if (!bookNotificationRepository.existsByUser_IdAndBook_Id(user.getId(), bookId)) {
            BookNotificationEntity notification = new BookNotificationEntity();
            notification.setUser(user);
            notification.setBook(book);
            bookNotificationRepository.save(notification);
        }
    }

    /**
     * Verwijdert het abonnement van de gebruiker voor het opgegeven boek.
     */
    @Transactional
    public void disable(String smartschoolUid, Long bookId) {
        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        bookNotificationRepository.deleteByUser_IdAndBook_Id(user.getId(), bookId);
    }

    /**
     * Geeft true terug als de gebruiker een actief abonnement heeft voor het opgegeven boek.
     *
     * @return true als abonnement bestaat, false als de gebruiker niet gevonden wordt of niet geabonneerd is
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(String smartschoolUid, Long bookId) {
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .map(user -> bookNotificationRepository.existsByUser_IdAndBook_Id(user.getId(), bookId))
                .orElse(false);
    }

    /**
     * Abonneert de gebruiker op alle boeken in de opgegeven lijst.
     * Al bestaande abonnementen worden overgeslagen.
     */
    @Transactional
    public void enableBulk(String smartschoolUid, List<Long> bookIds) {
        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        List<BookEntity> books = bookRepository.findAllById(bookIds);
        for (BookEntity book : books) {
            if (!bookNotificationRepository.existsByUser_IdAndBook_Id(user.getId(), book.getId())) {
                BookNotificationEntity notification = new BookNotificationEntity();
                notification.setUser(user);
                notification.setBook(book);
                bookNotificationRepository.save(notification);
            }
        }
    }

    /**
     * Verwijdert de abonnementen van de gebruiker voor alle opgegeven boeken.
     */
    @Transactional
    public void disableBulk(String smartschoolUid, List<Long> bookIds) {
        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        for (Long bookId : bookIds) {
            bookNotificationRepository.deleteByUser_IdAndBook_Id(user.getId(), bookId);
        }
    }

    /**
     * Geeft true terug als de gebruiker op alle opgegeven boeken een actief abonnement heeft.
     *
     * @return true als alle boeken geabonneerd zijn, false als de lijst leeg is of een boek ontbreekt
     */
    @Transactional(readOnly = true)
    public boolean isAllEnabled(String smartschoolUid, List<Long> bookIds) {
        if (bookIds.isEmpty()) return false;
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .map(user -> bookIds.stream()
                        .allMatch(bookId -> bookNotificationRepository.existsByUser_IdAndBook_Id(user.getId(), bookId)))
                .orElse(false);
    }

    /**
     * Verstuurt beschikbaarheidsberichten naar alle geabonneerde gebruikers van de opgegeven school en verwijdert daarna de abonnementen.
     * Loopt asynchroon zodat de retourregistratie niet geblokkeerd wordt.
     * Bekende beperking: abonnementen worden ook verwijderd als het bericht niet verstuurd kon worden.
     */
    @Async
    @Transactional
    public void triggerNotificationsForBook(BookEntity book, Long schoolId) {
        List<BookNotificationEntity> notifications = bookNotificationRepository
                .findAllByBook_IdAndUser_School_Id(book.getId(), schoolId);

        for (BookNotificationEntity notification : notifications) {
            messageService.sendMessage(
                    notification.getUser(),
                    "Boek terug beschikbaar",
                    "Let op: Dit betreft een geautomatiseerd bericht; reacties worden niet in behandeling genomen. GoSmartBib vraagt u nooit om op links te klikken via een e-mail.\n" +
                            "Het boek '" + book.getTitle() + "' is terug beschikbaar in de bibliotheek."
            );
        }
        bookNotificationRepository.deleteAll(notifications);
    }

}
