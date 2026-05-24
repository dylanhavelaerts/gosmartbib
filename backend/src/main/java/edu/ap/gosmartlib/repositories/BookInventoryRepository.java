package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.BookInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookInventoryRepository extends JpaRepository<BookInventoryEntity, Long> {
    void deleteAllBySchool_Id(Long schoolId);
}