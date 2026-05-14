package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.PurchaseRequestEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequestEntity, Long> {
    List<PurchaseRequestEntity> findByUser_School_Id(Long schoolId);

    @Modifying
    @Query("UPDATE PurchaseRequestEntity p SET p.user = null WHERE p.user = :user")
    void anonymizeByUser(@Param("user") UserEntity user);
}
