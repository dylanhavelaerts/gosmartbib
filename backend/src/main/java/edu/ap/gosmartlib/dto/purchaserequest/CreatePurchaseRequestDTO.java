package edu.ap.gosmartlib.dto.purchaserequest;

import java.util.List;

public record CreatePurchaseRequestDTO(
        String title,
        List<String> authors,
        String isbn
) {}
