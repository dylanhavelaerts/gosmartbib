package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findBySmartschoolUid(String smartschoolUid);

  @EntityGraph(attributePaths = { "school", "classes" })
  Optional<UserEntity> findDetailedBySmartschoolUid(String smartschoolUid);

  @EntityGraph(attributePaths = { "school", "classes" })
  List<UserEntity> findAllBySchool_IdOrderBySmartschoolUidAsc(Long schoolId);

  @Query("SELECT u FROM UserEntity u WHERE u.school.id = :schoolId AND u.role <> edu.ap.gosmartlib.util.UserRoles.ADMIN AND (:name IS NULL OR LOWER(u.smartschoolUid) LIKE LOWER(CONCAT('%', :name, '%')))")
  Page<UserEntity> findBySchoolIdAndName(Long schoolId, String name, Pageable pageable);

  @EntityGraph(attributePaths = { "school", "classes" })
  Optional<UserEntity> findByIdAndSchool_Id(Long id, Long schoolId);

  List<UserEntity> findAllBySchool_IdAndSmartschoolUidIn(
      Long schoolId,
      Collection<String> smartschoolUids);

  @Query("""
      SELECT COUNT(u)
      FROM UserEntity u
      WHERE u.school.id = :schoolId
        AND u.role = edu.ap.gosmartlib.util.UserRoles.STUDENT
         AND (:className IS NULL OR EXISTS (
          SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.name = :className))
        AND (:grade IS NULL OR EXISTS (
          SELECT sc FROM SchoolClassEntity sc WHERE sc MEMBER OF u.classes AND sc.grade = :grade))
        AND u.smartschoolUid NOT IN (
            SELECT l.smartschoolUserId FROM LoanEntity l
            JOIN UserEntity lu ON lu.smartschoolUid = l.smartschoolUserId
            WHERE lu.school.id = :schoolId
        )
        AND u.smartschoolUid NOT IN (
            SELECT lh.smartschoolUserId FROM LoanHistoryEntity lh
            JOIN UserEntity lhu ON lhu.smartschoolUid = lh.smartschoolUserId
            WHERE lhu.school.id = :schoolId AND lh.returnDate >= :since
        )
      """)
  long countInactiveStudents(@Param("schoolId") Long schoolId, @Param("since") LocalDate since,
      @Param("className") String className, @Param("grade") String grade);
}
