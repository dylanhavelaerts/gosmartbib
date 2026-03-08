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


    @Query("SELECT DISTINCT b FROM BookEntity b " +
            "LEFT JOIN b.authors a " +
            "LEFT JOIN b.categories c " +
            "WHERE (:language IS NULL OR LOWER(b.language) = LOWER(:language))" +
            "AND (:category IS NULL OR LOWER(c) LIKE LOWER(CONCAT('%', :category, '%')))" +
            "AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount) " +
            "AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount) " +
            "AND (:minPubYear IS NULL OR b.publishedYear <= :minPubYear) " +
            "AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)"
    )
    List<BookEntity> filterBooks(
            @Param("language") String language,
            @Param("category") String category,
            @Param("minPageCount") Integer minPageCount,
            @Param("maxPageCount") Integer maxPageCount,
            @Param("minPubYear") Integer minPubYear,
            @Param("maxPubYear") Integer maxPubYear);
}
