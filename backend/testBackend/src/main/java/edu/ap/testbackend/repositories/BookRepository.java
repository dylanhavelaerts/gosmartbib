package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.BookEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

    List<BookEntity> findTop4BySpotlightTrueOrderByIdDesc();

    List<BookEntity> findTop4ByOrderByIdDesc();

    @Query("SELECT DISTINCT b FROM BookEntity b JOIN b.authors a WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<BookEntity> searchByTitleOrAuthor(@Param("query") String query);
}
