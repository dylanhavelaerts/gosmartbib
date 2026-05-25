package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.entities.bookEntities.BookEntity;
import edu.ap.gosmartlib.entities.bookEntities.BookInventoryEntity;
import edu.ap.gosmartlib.repositories.bookRepositories.BookRepository;
import edu.ap.gosmartlib.util.BookCopyCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryAdjustmentService {

    private final BookRepository bookRepository;

    public void adjustForConditionChange(BookInventoryEntity inventory,
                                         BookCopyCondition from, BookCopyCondition to) {

        BookEntity book = inventory.getBook();

        switch (to) {
            case BROKEN -> {
                if (from == BookCopyCondition.GOOD || from == BookCopyCondition.DAMAGED) {
                    inventory.setAvailableCopies(Math.max(0, inventory.getAvailableCopies() - 1));
                    book.setAvailableCopies(Math.max(0, book.getAvailableCopies() - 1));
                }
            }
            case LOST -> {
                if (from == BookCopyCondition.GOOD || from == BookCopyCondition.DAMAGED) {
                    inventory.setAvailableCopies(Math.max(0, inventory.getAvailableCopies() - 1));
                    inventory.setTotalCopies(Math.max(0, inventory.getTotalCopies() - 1));
                    book.setAvailableCopies(Math.max(0, book.getAvailableCopies() - 1));
                    book.setTotalCopies(Math.max(0, book.getTotalCopies() - 1));
                } else if (from == BookCopyCondition.BROKEN) {
                    inventory.setTotalCopies(Math.max(0, inventory.getTotalCopies() - 1));
                    book.setTotalCopies(Math.max(0, book.getTotalCopies() - 1));
                }
            }
            case GOOD, DAMAGED -> {
                if (from == BookCopyCondition.BROKEN) {
                    inventory.setAvailableCopies(inventory.getAvailableCopies() + 1);
                    book.setAvailableCopies(book.getAvailableCopies() + 1);
                } else if (from == BookCopyCondition.LOST) {
                    inventory.setAvailableCopies(inventory.getAvailableCopies() + 1);
                    inventory.setTotalCopies(inventory.getTotalCopies() + 1);
                    book.setAvailableCopies(book.getAvailableCopies() + 1);
                    book.setTotalCopies(book.getTotalCopies() + 1);
                }
            }
        }

        bookRepository.save(book);
    }
}
