package edu.ap.gosmartlib.repositories.bookRepositories;

import edu.ap.gosmartlib.entities.BookEntities.BookCopyEntity;
import edu.ap.gosmartlib.entities.BookEntities.BookInventoryEntity;
import edu.ap.gosmartlib.util.BookCopyCondition;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends CrudRepository<BookCopyEntity, Long> {

    List<BookCopyEntity> findByInventory(BookInventoryEntity inventory);

    Optional<BookCopyEntity> findByBarcode(String barcode);

    long countByInventoryAndCondition(BookInventoryEntity inventory, BookCopyCondition condition);

    List<BookCopyEntity> findByInventoryAndCondition(BookInventoryEntity inventory, BookCopyCondition condition);
}
