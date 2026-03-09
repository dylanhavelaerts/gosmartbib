package edu.ap.testbackend.repositories;

import edu.ap.testbackend.entities.BookEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

    List<BookEntity> findTop4BySpotlightTrueOrderByIdDesc();

    List<BookEntity> findBySpotlightTrueOrderByIdDesc();

    List<BookEntity> findTop4ByOrderByIdDesc();

    /**
     * Zoek boeken op titel of auteur, case-insensitive en ondersteunt gedeeltelijke overeenkomsten.
     * @param query
     * @return
     */
    @Query("SELECT DISTINCT b FROM BookEntity b JOIN b.authors a WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<BookEntity> searchByTitleOrAuthor(@Param("query") String query);


    @Query("""
        SELECT DISTINCT b FROM BookEntity b
        LEFT JOIN b.categories c
        WHERE (:language IS NULL OR LOWER(b.language) = LOWER(:language))
        AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
        AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
        AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
        AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
        AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
        """)
    List<BookEntity> filterBooks(
            @Param("language") String language,
            @Param("categories") List<String> categories,
            @Param("minPageCount") Integer minPageCount,
            @Param("maxPageCount") Integer maxPageCount,
            @Param("minPubYear") Integer minPubYear,
            @Param("maxPubYear") Integer maxPubYear);
}
