package edu.ap.gosmartlib.repositories;

import edu.ap.gosmartlib.entities.PurchaseRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequestEntity, Long> {
    List<PurchaseRequestEntity> findByUser_School_Id(Long schoolId);
}
