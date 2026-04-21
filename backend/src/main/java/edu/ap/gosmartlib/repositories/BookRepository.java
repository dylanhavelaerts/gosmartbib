package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.BookEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

        @Query(value = """
                        SELECT b FROM BookEntity b
                        WHERE (:canSeeDidactic = true OR b.didacticTag = false)
                        """, countQuery = """
                        SELECT COUNT(b) FROM BookEntity b
                        WHERE (:canSeeDidactic = true OR b.didacticTag = false)
                        """)
        Page<BookEntity> findAllFiltered(@Param("canSeeDidactic") boolean includeDidactic, Pageable pageable);

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        List<BookEntity> findTop4BySpotlightTrueOrderByIdDesc();

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        List<BookEntity> findBySpotlightTrueOrderByIdDesc();

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        List<BookEntity> findTop4ByOrderByIdDesc();

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        List<BookEntity> findTop4ByOrderByRatingDesc();

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        List<BookEntity> findTop4ByDidacticTagTrueOrderByRatingDesc();

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        List<BookEntity> findTop4ByAgeRangeIgnoreCaseAndDidacticTagFalseOrderByRatingDesc(String ageRange);

        boolean existsByIsbn(String isbn);

        /**
         * Zoek boeken op titel, auteur of ISBN, case-insensitive en ondersteunt
         * gedeeltelijke
         * overeenkomsten. Negeert streepjes bij het zoeken op ISBN.
         *
         * @param query
         * @return
         */
        @Query("SELECT DISTINCT b FROM BookEntity b LEFT JOIN b.authors a WHERE " +
                        "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(a) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "REPLACE(b.isbn, '-', '') LIKE CONCAT('%', REPLACE(:query, '-', ''), '%')")
        Page<BookEntity> searchByTitleOrAuthor(@Param("query") String query, Pageable pageable);

        /**
         * Zoek boeken op titel of auteur, case-insensitive en ondersteunt gedeeltelijke
         * overeenkomsten.
         *
         * @param query
         * @return
         */
        @Query("""
                        SELECT DISTINCT b FROM BookEntity b LEFT JOIN b.authors a LEFT JOIN b.categories c
                        WHERE (:includeDidactic = true OR b.didacticTag = false)
                        AND (
                            LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(a) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
                        )
                        """)
        Page<BookEntity> searchByTitleOrAuthorOrCategory(@Param("query") String query,
                        @Param("includeDidactic") boolean includeDidactic, Pageable pageable);

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        @Query("""
                        SELECT b
                        FROM BookEntity b
                        WHERE b.id = :id
                        """)
        Optional<BookEntity> findDetailedById(@Param("id") Long id);

        @EntityGraph(attributePaths = { "inventories", "inventories.school" })
        Optional<BookEntity> findByIsbn(String isbn);

        @Query(value = """
                        SELECT DISTINCT b FROM BookEntity b
                        LEFT JOIN b.categories c
                        LEFT JOIN b.labels l
                        WHERE (:language IS NULL OR LOWER(b.language) = LOWER(:language))
                        AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
                        AND (:#{#labels == null || #labels.isEmpty()} = true OR l IN :labels)
                        AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
                        AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
                        AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
                        AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
                        AND (:includeDidactic = true OR b.didacticTag = false)
                        """, countQuery = """
                        SELECT COUNT(DISTINCT b) FROM BookEntity b
                        LEFT JOIN b.categories c
                        LEFT JOIN b.labels l
                        WHERE (:includeDidactic = true OR b.didacticTag = false)
                        AND (:language IS NULL OR LOWER(b.language) = LOWER(:language))
                        AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
                        AND (:#{#labels == null || #labels.isEmpty()} = true OR l IN :labels)
                        AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
                        AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
                        AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
                        AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
                        """)
        Page<BookEntity> filterBooks(
                        @Param("includeDidactic") boolean includeDidactic,
                        @Param("language") String language,
                        @Param("categories") List<String> categories,
                        @Param("labels") List<String> labels,
                        @Param("minPageCount") Integer minPageCount,
                        @Param("maxPageCount") Integer maxPageCount,
                        @Param("minPubYear") Integer minPubYear,
                        @Param("maxPubYear") Integer maxPubYear,
                        Pageable pageable);
}