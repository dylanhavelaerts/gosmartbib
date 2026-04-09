package edu.ap.gosmartlib.dto.reviews;

import edu.ap.gosmartlib.util.ReviewFlagReason;

public record ReviewFlagRequestDTO(
        ReviewFlagReason reason
) {
}