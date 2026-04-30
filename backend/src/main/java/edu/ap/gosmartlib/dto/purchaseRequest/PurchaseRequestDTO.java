package edu.ap.gosmartlib.dto.purchaseRequest;

import edu.ap.gosmartlib.util.PurchaseStatus;

import java.time.LocalDate;
import java.util.List;

public record PurchaseRequestDTO(
        Long id,
        String title,
        List<String> authors,
        String isbn,
        PurchaseStatus status,
        Long userId,
        String userSmartschoolUid,
        LocalDate requestDate,
        String note
) {}
