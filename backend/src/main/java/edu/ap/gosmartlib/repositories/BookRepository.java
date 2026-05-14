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
  @Query("""
      SELECT DISTINCT b
      FROM BookEntity b
      WHERE b.spotlight = true
        AND (:readingLevel IS NULL OR LOWER(b.readingLevel) = LOWER(:readingLevel))
        AND (:includeDidactic = true OR b.didacticTag = false)
        AND (
            :schoolId IS NULL OR EXISTS (
                SELECT 1
                FROM BookInventoryEntity inv
                WHERE inv.book = b
                  AND inv.school.id = :schoolId
            )
        )
      ORDER BY b.id DESC
      """)
  List<BookEntity> findSpotlightBooksByReadingLevel(
      @Param("readingLevel") String readingLevel,
      @Param("includeDidactic") boolean includeDidactic,
      @Param("schoolId") Long schoolId,
      Pageable pageable);

  @EntityGraph(attributePaths = { "inventories", "inventories.school" })
  List<BookEntity> findBySpotlightTrueOrderByIdDesc();

  @EntityGraph(attributePaths = { "inventories", "inventories.school" })
  List<BookEntity> findTop4ByOrderByIdDesc();

  @EntityGraph(attributePaths = { "inventories", "inventories.school" })
  List<BookEntity> findTop4ByOrderByRatingDesc();

  @EntityGraph(attributePaths = { "inventories", "inventories.school" })
  @Query("""
      SELECT DISTINCT b
      FROM BookEntity b
      WHERE (:readingLevel IS NULL OR LOWER(b.readingLevel) = LOWER(:readingLevel))
        AND (:includeDidactic = true OR b.didacticTag = false)
        AND (
            :schoolId IS NULL OR EXISTS (
                SELECT 1
                FROM BookInventoryEntity inv
                WHERE inv.book = b
                  AND inv.school.id = :schoolId
            )
        )
      ORDER BY COALESCE(b.rating, 0) DESC, b.id DESC
      """)
  List<BookEntity> findTopRatedBooksByReadingLevel(
      @Param("readingLevel") String readingLevel,
      @Param("includeDidactic") boolean includeDidactic,
      @Param("schoolId") Long schoolId,
      Pageable pageable);

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
      LEFT JOIN b.authors a
      LEFT JOIN b.categories c
      LEFT JOIN b.labels l
      WHERE (:includeDidactic = true OR b.didacticTag = false)
      AND (
          :query IS NULL OR TRIM(:query) = '' OR
          LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(a) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
      )
      AND (:language IS NULL OR LOWER(b.language) = LOWER(:language))
      AND (:readingLevel IS NULL OR TRIM(:readingLevel) = '' OR LOWER(b.readingLevel) = LOWER(:readingLevel))
      AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
      AND (:#{#labels == null || #labels.isEmpty()} = true OR l IN :labels)
      AND (:didacticOnly = false OR b.didacticTag = true)
      AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
      AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
      AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
      AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
      AND (:minRating IS NULL OR COALESCE(b.rating, 0) >= :minRating)
      AND (:maxRating IS NULL OR COALESCE(b.rating, 0) <= :maxRating)
      """, countQuery = """
      SELECT COUNT(DISTINCT b) FROM BookEntity b
      LEFT JOIN b.authors a
      LEFT JOIN b.categories c
      LEFT JOIN b.labels l
      WHERE (:includeDidactic = true OR b.didacticTag = false)
      AND (
          :query IS NULL OR TRIM(:query) = '' OR
          LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(a) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
      )
      AND (:language IS NULL OR LOWER(b.language) = LOWER(:language))
      AND (:readingLevel IS NULL OR TRIM(:readingLevel) = '' OR LOWER(b.readingLevel) = LOWER(:readingLevel))
      AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
      AND (:#{#labels == null || #labels.isEmpty()} = true OR l IN :labels)
      AND (:didacticOnly = false OR b.didacticTag = true)
      AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
      AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
      AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
      AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
      AND (:minRating IS NULL OR COALESCE(b.rating, 0) >= :minRating)
      AND (:maxRating IS NULL OR COALESCE(b.rating, 0) <= :maxRating)
      """)
  Page<BookEntity> filterBooks(
      @Param("includeDidactic") boolean includeDidactic,
      @Param("query") String query,
      @Param("language") String language,
      @Param("categories") List<String> categories,
      @Param("labels") List<String> labels,
      @Param("readingLevel") String readingLevel,
      @Param("didacticOnly") boolean didacticOnly,
      @Param("minPageCount") Integer minPageCount,
      @Param("maxPageCount") Integer maxPageCount,
      @Param("minPubYear") Integer minPubYear,
      @Param("maxPubYear") Integer maxPubYear,
      @Param("minRating") Double minRating,
      @Param("maxRating") Double maxRating,
      Pageable pageable);

  @Query(value = """
      SELECT DISTINCT b
      FROM BookEntity b
      WHERE (:includeDidactic = true OR b.didacticTag = false)
        AND EXISTS (
            SELECT 1
            FROM BookInventoryEntity inv
            WHERE inv.book = b
              AND inv.school.id = :schoolId
        )
      """, countQuery = """
      SELECT COUNT(DISTINCT b)
      FROM BookEntity b
      WHERE (:includeDidactic = true OR b.didacticTag = false)
        AND EXISTS (
            SELECT 1
            FROM BookInventoryEntity inv
            WHERE inv.book = b
              AND inv.school.id = :schoolId
        )
      """)
  Page<BookEntity> findAllFilteredForSchool(
      @Param("includeDidactic") boolean includeDidactic,
      @Param("schoolId") Long schoolId,
      Pageable pageable);

  @Query(value = """
      SELECT DISTINCT b
      FROM BookEntity b
      LEFT JOIN b.authors a
      LEFT JOIN b.categories c
      WHERE (:includeDidactic = true OR b.didacticTag = false)
        AND EXISTS (
            SELECT 1
            FROM BookInventoryEntity inv
            WHERE inv.book = b
              AND inv.school.id = :schoolId
        )
        AND (
            LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(a) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
        )
      """, countQuery = """
      SELECT COUNT(DISTINCT b)
      FROM BookEntity b
      LEFT JOIN b.authors a
      LEFT JOIN b.categories c
      WHERE (:includeDidactic = true OR b.didacticTag = false)
        AND EXISTS (
            SELECT 1
            FROM BookInventoryEntity inv
            WHERE inv.book = b
              AND inv.school.id = :schoolId
        )
        AND (
            LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(a) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
        )
      """)
  Page<BookEntity> searchByTitleOrAuthorOrCategoryForSchool(
      @Param("query") String query,
      @Param("includeDidactic") boolean includeDidactic,
      @Param("schoolId") Long schoolId,
      Pageable pageable);

  // Snowball effect voor boeken van dezelfde auteurs, maar niet hetzelfde boek
  // (om te voorkomen dat het boek zelf als aanbeveling verschijnt)
  @EntityGraph(attributePaths = { "inventories", "inventories.school" })
  @Query("""
          SELECT DISTINCT b FROM BookEntity b
          JOIN b.authors a
          WHERE a IN :authors AND b.id != :excludeId
      """)
  List<BookEntity> findByAuthorsInAndIdNot(
      @Param("authors") List<String> authors,
      @Param("excludeId") Long excludeId,
      Pageable pageable);

  // Snowball effect voor boeken in dezelfde categorieën, maar niet hetzelfde boek
  // (om te voorkomen dat het boek zelf als aanbeveling verschijnt)
  @EntityGraph(attributePaths = { "inventories", "inventories.school" })
  @Query("""
          SELECT DISTINCT b FROM BookEntity b
          JOIN b.categories c
          WHERE c IN :categories AND b.id != :excludeId
      """)
  List<BookEntity> findByCategoriesInAndIdNot(
      @Param("categories") List<String> categories,
      @Param("excludeId") Long excludeId,
      Pageable pageable);

  @Query(value = """
      SELECT DISTINCT b FROM BookEntity b
      LEFT JOIN b.authors a
      LEFT JOIN b.categories c
      LEFT JOIN b.labels l
      WHERE (:includeDidactic = true OR b.didacticTag = false)
      AND EXISTS (
          SELECT 1
          FROM BookInventoryEntity inv
          WHERE inv.book = b
            AND inv.school.id = :schoolId
      )
      AND (
          :query IS NULL OR TRIM(:query) = '' OR
          LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(a) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
      )
      AND (:language IS NULL OR LOWER(b.language) = LOWER(:language))
      AND (:readingLevel IS NULL OR TRIM(:readingLevel) = '' OR LOWER(b.readingLevel) = LOWER(:readingLevel))
      AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
      AND (:#{#labels == null || #labels.isEmpty()} = true OR l IN :labels)
      AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
      AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
      AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
      AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
      AND (:minRating IS NULL OR COALESCE(b.rating, 0) >= :minRating)
      AND (:maxRating IS NULL OR COALESCE(b.rating, 0) <= :maxRating)
      """, countQuery = """
      SELECT COUNT(DISTINCT b) FROM BookEntity b
      LEFT JOIN b.authors a
      LEFT JOIN b.categories c
      LEFT JOIN b.labels l
      WHERE (:includeDidactic = true OR b.didacticTag = false)
      AND EXISTS (
          SELECT 1
          FROM BookInventoryEntity inv
          WHERE inv.book = b
            AND inv.school.id = :schoolId
      )
      AND (
          :query IS NULL OR TRIM(:query) = '' OR
          LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(a) LIKE LOWER(CONCAT('%', :query, '%')) OR
          LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
      )
      AND (:language IS NULL OR LOWER(b.language) = LOWER(:language))
      AND (:readingLevel IS NULL OR TRIM(:readingLevel) = '' OR LOWER(b.readingLevel) = LOWER(:readingLevel))
      AND (:#{#categories == null || #categories.isEmpty()} = true OR c IN :categories)
      AND (:#{#labels == null || #labels.isEmpty()} = true OR l IN :labels)
      AND (:minPageCount IS NULL OR b.pageCount >= :minPageCount)
      AND (:maxPageCount IS NULL OR b.pageCount <= :maxPageCount)
      AND (:minPubYear IS NULL OR b.publishedYear >= :minPubYear)
      AND (:maxPubYear IS NULL OR b.publishedYear <= :maxPubYear)
      AND (:minRating IS NULL OR COALESCE(b.rating, 0) >= :minRating)
      AND (:maxRating IS NULL OR COALESCE(b.rating, 0) <= :maxRating)
      """)
  Page<BookEntity> filterBooksForSchool(
      @Param("includeDidactic") boolean includeDidactic,
      @Param("query") String query,
      @Param("language") String language,
      @Param("categories") List<String> categories,
      @Param("labels") List<String> labels,
      @Param("readingLevel") String readingLevel,
      @Param("minPageCount") Integer minPageCount,
      @Param("maxPageCount") Integer maxPageCount,
      @Param("minPubYear") Integer minPubYear,
      @Param("maxPubYear") Integer maxPubYear,
      @Param("minRating") Double minRating,
      @Param("maxRating") Double maxRating,
      @Param("schoolId") Long schoolId,
      Pageable pageable);
}
