package edu.ap.gosmartlib.repositories.bookRepositories;

import edu.ap.gosmartlib.entities.BookEntities.BookInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookInventoryRepository extends JpaRepository<BookInventoryEntity, Long> {
    void deleteAllBySchool_Id(Long schoolId);
}