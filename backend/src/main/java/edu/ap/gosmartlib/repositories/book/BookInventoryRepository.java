package edu.ap.gosmartlib.repositories.book;

import edu.ap.gosmartlib.entities.book.BookInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BookInventoryRepository extends JpaRepository<BookInventoryEntity, Long> {
    void deleteAllBySchool_Id(Long schoolId);
    List<BookInventoryEntity> findBySchool_Id(Long schoolId);
}