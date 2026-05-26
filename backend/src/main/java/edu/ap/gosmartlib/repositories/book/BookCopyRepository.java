package edu.ap.gosmartlib.repositories.book;

import edu.ap.gosmartlib.entities.book.BookCopyEntity;
import edu.ap.gosmartlib.entities.book.BookInventoryEntity;
import edu.ap.gosmartlib.util.BookCopyCondition;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends CrudRepository<BookCopyEntity, Long> {

    List<BookCopyEntity> findByInventory(BookInventoryEntity inventory);

    Optional<BookCopyEntity> findByBarcode(String barcode);

    List<BookCopyEntity> findByInventoryAndCopyConditionNot(BookInventoryEntity inventory, BookCopyCondition condition);

    List<BookCopyEntity> findByInventoryAndCopyCondition(BookInventoryEntity inventory, BookCopyCondition condition);

    long countByInventoryAndCopyCondition(BookInventoryEntity inventory, BookCopyCondition condition);

    long countByInventoryAndCopyConditionNot(BookInventoryEntity inventory, BookCopyCondition condition);

    long countByInventory(BookInventoryEntity inventory);

}
