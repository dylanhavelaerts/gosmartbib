package edu.ap.gosmartlib.dto;

import java.util.List;

public record BookFilterRequest(String query,
                                String language,
                                List<String> categories,
                                List<String> labels,
                                Integer minPageCount,
                                Integer maxPageCount,
                                Integer minPubYear,
                                Integer maxPubYear,
                                Double minRating,
                                Double maxRating,
                                int page,
                                int size
) {
}