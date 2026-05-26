package edu.ap.gosmartlib.services.loans;

import edu.ap.gosmartlib.dto.loan.LibrarianActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LibrarianLoanHistoryDTO;
import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.loan.LoanEntity;
import edu.ap.gosmartlib.entities.loan.LoanExtensionStatus;
import edu.ap.gosmartlib.entities.loan.LoanHistoryEntity;
import edu.ap.gosmartlib.util.BookDisplayUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AdminLoanMapper {

    public LibrarianActiveLoanDTO toActiveDTO(
            LoanEntity loan,
            Map<String, String> displayNames,
            Map<String, List<String>> classMap,
            Map<String, BookEntity> bookMap) {

        String uid = loan.getSmartschoolUserId();
        BookEntity book = bookMap.get(loan.getIsbn());
        String extensionStatus = loan.getExtensionStatus() != null
                ? loan.getExtensionStatus().name() : LoanExtensionStatus.NONE.name();

        return new LibrarianActiveLoanDTO(
                loan.getId(), uid,
                displayNames.getOrDefault(uid, "Leerling"),
                classMap.getOrDefault(uid, List.of()),
                loan.getQuantity(), loan.getLoanDate(), loan.getDueDate(),
                extensionStatus, toBookDTO(book));
    }

    public LibrarianLoanHistoryDTO toHistoryDTO(
            LoanHistoryEntity history,
            Map<String, String> displayNames,
            Map<String, List<String>> classMap,
            Map<String, BookEntity> bookMap) {

        String uid = history.getSmartschoolUserId();
        BookEntity book = bookMap.get(history.getIsbn());

        String bookTitle = BookDisplayUtil.resolveTitle(book, history.getIsbn());
        String author = BookDisplayUtil.resolveAuthor(book);

        return new LibrarianLoanHistoryDTO(
                history.getId(), bookTitle, author,
                history.getLoanDate(), history.getReturnDate(), history.getQuantity(),
                displayNames.getOrDefault(uid, "Leerling"),
                classMap.getOrDefault(uid, List.of()),
                history.getDamagedCount(),
                history.getBrokenCount(),
                history.getLostCount());
    }

    private LibrarianActiveLoanDTO.LoanBookDTO toBookDTO(BookEntity book) {
        if (book == null) return null;
        List<String> authors = book.getAuthors() != null
                ? new ArrayList<>(book.getAuthors()) : new ArrayList<>();
        return new LibrarianActiveLoanDTO.LoanBookDTO(
                book.getId(), book.getTitle(), book.getThumbnail(), book.getIsbn(), authors);
    }
}
