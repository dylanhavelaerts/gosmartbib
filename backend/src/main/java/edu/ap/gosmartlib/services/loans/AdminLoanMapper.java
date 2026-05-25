package edu.ap.gosmartlib.services.loans;

import edu.ap.gosmartlib.dto.loan.AdminActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.AdminLoanHistoryDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanEntity;
import edu.ap.gosmartlib.entities.LoanEntities.LoanExtensionStatus;
import edu.ap.gosmartlib.entities.LoanEntities.LoanHistoryEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AdminLoanMapper {

    public AdminActiveLoanDTO toActiveDTO(
            LoanEntity loan,
            Map<String, String> displayNames,
            Map<String, List<String>> classMap,
            Map<String, BookEntity> bookMap) {

        String uid = loan.getSmartschoolUserId();
        BookEntity book = bookMap.get(loan.getIsbn());
        String extensionStatus = loan.getExtensionStatus() != null
                ? loan.getExtensionStatus().name() : LoanExtensionStatus.NONE.name();

        return new AdminActiveLoanDTO(
                loan.getId(), uid,
                displayNames.getOrDefault(uid, "Leerling"),
                classMap.getOrDefault(uid, List.of()),
                loan.getQuantity(), loan.getLoanDate(), loan.getDueDate(),
                extensionStatus, toBookDTO(book));
    }

    public AdminLoanHistoryDTO toHistoryDTO(
            LoanHistoryEntity history,
            Map<String, String> displayNames,
            Map<String, List<String>> classMap,
            Map<String, BookEntity> bookMap) {

        String uid = history.getSmartschoolUserId();
        BookEntity book = bookMap.get(history.getIsbn());

        String bookTitle;
        String author;
        if (book != null) {
            bookTitle = book.getTitle();
            author = String.join(", ", book.getAuthors());
        } else {
            bookTitle = "Onbekend Boek (ISBN: " + history.getIsbn() + ")";
            author = "Onbekende Auteur";
        }

        return new AdminLoanHistoryDTO(
                history.getId(), bookTitle, author,
                history.getLoanDate(), history.getReturnDate(), history.getQuantity(),
                displayNames.getOrDefault(uid, "Leerling"),
                classMap.getOrDefault(uid, List.of()));
    }

    private AdminActiveLoanDTO.LoanBookDTO toBookDTO(BookEntity book) {
        if (book == null) return null;
        List<String> authors = book.getAuthors() != null
                ? new ArrayList<>(book.getAuthors()) : new ArrayList<>();
        return new AdminActiveLoanDTO.LoanBookDTO(
                book.getId(), book.getTitle(), book.getThumbnail(), book.getIsbn(), authors);
    }
}
