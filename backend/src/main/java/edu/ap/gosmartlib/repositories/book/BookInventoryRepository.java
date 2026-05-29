package edu.ap.gosmartlib.repositories.book;

import edu.ap.gosmartlib.entities.book.BookInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookInventoryRepository extends JpaRepository<BookInventoryEntity, Long> {
    void deleteAllBySchool_Id(Long schoolId);

    List<BookInventoryEntity> findBySchool_Id(Long schoolId);

    boolean existsBySchool_IdAndCampusIgnoreCase(Long schoolId, String campus);

    @Modifying
    @Query("""
            UPDATE BookInventoryEntity inventory
            SET inventory.campus = :newCampus
            WHERE inventory.school.id = :schoolId
              AND LOWER(inventory.campus) = LOWER(:oldCampus)
            """)
    int renameCampusForSchool(
            @Param("schoolId") Long schoolId,
            @Param("oldCampus") String oldCampus,
            @Param("newCampus") String newCampus);
}