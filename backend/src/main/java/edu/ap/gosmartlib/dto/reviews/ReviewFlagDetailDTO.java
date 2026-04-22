package edu.ap.gosmartlib.dto.reviews;

import edu.ap.gosmartlib.util.ReviewFlagReason;

public record ReviewFlagDetailDTO(
        String flaggerUid,
        ReviewFlagReason reason
) {
}
