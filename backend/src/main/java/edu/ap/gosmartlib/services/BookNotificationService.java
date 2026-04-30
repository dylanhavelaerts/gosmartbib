package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.BookNotificationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookNotificationRepository;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class BookNotificationService {

    private final BookNotificationRepository bookNotificationRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final SmartschoolMessageService messageService;

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

    @Transactional
    public void disable(String smartschoolUid, Long bookId) {
        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        bookNotificationRepository.deleteByUser_IdAndBook_Id(user.getId(), bookId);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String smartschoolUid, Long bookId) {
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .map(user -> bookNotificationRepository.existsByUser_IdAndBook_Id(user.getId(), bookId))
                .orElse(false);
    }

    @Async
    @Transactional
    public void triggerNotificationsForBook(BookEntity book, Long schoolId) {
        List<BookNotificationEntity> notifications = bookNotificationRepository
                .findAllByBook_IdAndUser_School_Id(book.getId(), schoolId);

        for (BookNotificationEntity notification : notifications) {
            messageService.sendMessage(
                    notification.getUser(),
                    "Boek terug beschikbaar",
                    "Het boek '" + book.getTitle() + "' is terug beschikbaar in de bibliotheek."
            );
            bookNotificationRepository.delete(notification);
        }
    }
}
