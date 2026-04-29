package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.UserEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findBySmartschoolUid(String smartschoolUid);

    /**
     * Laad de hele user met alle nodige details
     */
    @EntityGraph(attributePaths = { "school", "classes" })
    Optional<UserEntity> findDetailedBySmartschoolUid(String smartschoolUid);

    /**
     * Vindt alle users van een zekere school
     */
    @EntityGraph(attributePaths = { "school", "classes" })
    List<UserEntity> findAllBySchool_IdAndActiveIsTrueOrderBySmartschoolUidAsc(Long schoolId);

    @Query("SELECT u FROM UserEntity u WHERE u.school.id = :schoolId AND u.active = true AND (:name IS NULL OR LOWER(u.smartschoolUid) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<UserEntity> findBySchoolIdAndName(Long schoolId, String name, Pageable pageable);

    /**
     * Vindt één specifieke user van een zekere school (scope zo klein mogelijk
     * houden)
     */
    @EntityGraph(attributePaths = { "school", "classes" })
    Optional<UserEntity> findByIdAndSchool_Id(Long id, Long schoolId);

    /**
     * Vindt actieve users van een school op basis van hun smartschoolUid.
     * Dit gebruiken we om display names alleen op te lossen voor users die
     * al in onze eigen database en in dezelfde school gekend zijn.
     */
    List<UserEntity> findAllBySchool_IdAndSmartschoolUidInAndActiveIsTrue(
            Long schoolId,
            Collection<String> smartschoolUids);

    List<UserEntity> findAllByActiveIsFalseAndScheduledDeletionAtBefore(LocalDateTime cutoff);
}