package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.BookEntity;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

        Page<BookEntity> findAll(Pageable pageable);

        List<BookEntity> findTop4BySpotlightTrueOrderByIdDesc();

        List<BookEntity> findBySpotlightTrueOrderByIdDesc();

        List<BookEntity> findTop4ByOrderByIdDesc();

        List<BookEntity> findTop4ByOrderByRatingDesc();

        boolean existsByIsbn(String isbn);

        /**
         * Zoek boeken op titel of auteur, case-insensitive en ondersteunt gedeeltelijke
         * overeenkomsten.
         * 
         * @param query
         * @return
         */
        @Query("SELECT DISTINCT b FROM BookEntity b LEFT JOIN b.authors a LEFT JOIN b.categories c WHERE " +
                        "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))" +
                        " OR LOWER(a) LIKE LOWER(CONCAT('%', :query, '%'))" +
                        " OR LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))")
        Page<BookEntity> searchByTitleOrAuthorOrCategory(@Param("query") String query, Pageable pageable);

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
                        """, countQuery = """
                        SELECT COUNT(DISTINCT b) FROM BookEntity b
                        LEFT JOIN b.categories c
                        LEFT JOIN b.labels l
                        WHERE (:language IS NULL OR LOWER(b.language) = LOWER(:language))
                        AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
                        AND (:#{#labels == null || #labels.isEmpty()} = true OR l IN :labels)
                        AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
                        AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
                        AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
                        AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
                        """)
        Page<BookEntity> filterBooks(
                        @Param("language") String language,
                        @Param("categories") List<String> categories,
                        @Param("labels") List<String> labels,
                        @Param("minPageCount") Integer minPageCount,
                        @Param("maxPageCount") Integer maxPageCount,
                        @Param("minPubYear") Integer minPubYear,
                        @Param("maxPubYear") Integer maxPubYear,
                        Pageable pageable);
}
