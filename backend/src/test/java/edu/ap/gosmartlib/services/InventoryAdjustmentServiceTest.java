package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.book.BookInventoryEntity;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.util.BookCopyCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentServiceTest {

    @Mock private BookRepository bookRepository;

    @InjectMocks private InventoryAdjustmentService inventoryAdjustmentService;

    private BookEntity book;
    private BookInventoryEntity inventory;

    @BeforeEach
    void setUp() {
        book = new BookEntity();
        book.setTotalCopies(5);
        book.setAvailableCopies(3);

        inventory = new BookInventoryEntity();
        inventory.setBook(book);
        inventory.setTotalCopies(5);
        inventory.setAvailableCopies(3);

        when(bookRepository.save(any())).thenReturn(book);
    }

    // --- To BROKEN ---

    @Test
    void givenGoodCondition_whenChangedToBroken_thenDecrementsAvailableOnInventoryAndBook() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.GOOD, BookCopyCondition.BROKEN);

        assertEquals(2, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
        assertEquals(2, book.getAvailableCopies());
        assertEquals(5, book.getTotalCopies());
        verify(bookRepository).save(book);
    }

    @Test
    void givenDamagedCondition_whenChangedToBroken_thenDecrementsAvailableOnInventoryAndBook() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.DAMAGED, BookCopyCondition.BROKEN);

        assertEquals(2, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
        assertEquals(2, book.getAvailableCopies());
        assertEquals(5, book.getTotalCopies());
    }

    @Test
    void givenBrokenCondition_whenChangedToBroken_thenNoCountsChange() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.BROKEN, BookCopyCondition.BROKEN);

        assertEquals(3, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
        assertEquals(3, book.getAvailableCopies());
        assertEquals(5, book.getTotalCopies());
    }

    @Test
    void givenZeroAvailable_whenChangedToBroken_thenAvailableDoesNotGoBelowZero() {
        inventory.setAvailableCopies(0);
        book.setAvailableCopies(0);

        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.GOOD, BookCopyCondition.BROKEN);

        assertEquals(0, inventory.getAvailableCopies());
        assertEquals(0, book.getAvailableCopies());
    }

    // --- To LOST ---

    @Test
    void givenGoodCondition_whenChangedToLost_thenDecrementsAvailableAndTotal() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.GOOD, BookCopyCondition.LOST);

        assertEquals(2, inventory.getAvailableCopies());
        assertEquals(4, inventory.getTotalCopies());
        assertEquals(2, book.getAvailableCopies());
        assertEquals(4, book.getTotalCopies());
    }

    @Test
    void givenDamagedCondition_whenChangedToLost_thenDecrementsAvailableAndTotal() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.DAMAGED, BookCopyCondition.LOST);

        assertEquals(2, inventory.getAvailableCopies());
        assertEquals(4, inventory.getTotalCopies());
        assertEquals(2, book.getAvailableCopies());
        assertEquals(4, book.getTotalCopies());
    }

    @Test
    void givenBrokenCondition_whenChangedToLost_thenDecrementsOnlyTotal() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.BROKEN, BookCopyCondition.LOST);

        assertEquals(3, inventory.getAvailableCopies());
        assertEquals(4, inventory.getTotalCopies());
        assertEquals(3, book.getAvailableCopies());
        assertEquals(4, book.getTotalCopies());
    }

    @Test
    void givenLostCondition_whenChangedToLost_thenNoCountsChange() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.LOST, BookCopyCondition.LOST);

        assertEquals(3, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
    }

    // --- To GOOD ---

    @Test
    void givenBrokenCondition_whenChangedToGood_thenIncrementsAvailable() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.BROKEN, BookCopyCondition.GOOD);

        assertEquals(4, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
        assertEquals(4, book.getAvailableCopies());
        assertEquals(5, book.getTotalCopies());
    }

    @Test
    void givenLostCondition_whenChangedToGood_thenIncrementsAvailableAndTotal() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.LOST, BookCopyCondition.GOOD);

        assertEquals(4, inventory.getAvailableCopies());
        assertEquals(6, inventory.getTotalCopies());
        assertEquals(4, book.getAvailableCopies());
        assertEquals(6, book.getTotalCopies());
    }

    @Test
    void givenGoodCondition_whenChangedToGood_thenNoCountsChange() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.GOOD, BookCopyCondition.GOOD);

        assertEquals(3, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
    }

    // --- To DAMAGED ---

    @Test
    void givenBrokenCondition_whenChangedToDamaged_thenIncrementsAvailable() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.BROKEN, BookCopyCondition.DAMAGED);

        assertEquals(4, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
        assertEquals(4, book.getAvailableCopies());
        assertEquals(5, book.getTotalCopies());
    }

    @Test
    void givenLostCondition_whenChangedToDamaged_thenIncrementsAvailableAndTotal() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.LOST, BookCopyCondition.DAMAGED);

        assertEquals(4, inventory.getAvailableCopies());
        assertEquals(6, inventory.getTotalCopies());
        assertEquals(4, book.getAvailableCopies());
        assertEquals(6, book.getTotalCopies());
    }

    @Test
    void givenDamagedCondition_whenChangedToDamaged_thenNoCountsChange() {
        inventoryAdjustmentService.adjustForConditionChange(inventory, BookCopyCondition.DAMAGED, BookCopyCondition.DAMAGED);

        assertEquals(3, inventory.getAvailableCopies());
        assertEquals(5, inventory.getTotalCopies());
    }
}
