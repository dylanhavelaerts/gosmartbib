package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.BookFilterRequest;
import edu.ap.gosmartlib.exceptions.NegativeValueException;
import org.springframework.stereotype.Component;

@Component
public class BookFilterValidator {

    public void validate(BookFilterRequest request) {
        if (request.page() < 0 || request.size() <= 0)
            throw new NegativeValueException("Paginanummer mag niet negatief zijn en de grootte moet groter zijn dan 0");

        if (request.minPageCount() != null && request.maxPageCount() != null && request.minPageCount() > request.maxPageCount())
            throw new IllegalArgumentException("minPageCount mag niet groter zijn dan maxPageCount");

        if (request.minPubYear() != null && request.maxPubYear() != null && request.minPubYear() > request.maxPubYear())
            throw new IllegalArgumentException("minPubYear mag niet groter zijn dan maxPubYear");

        if (request.minRating() != null && (request.minRating() < 1 || request.minRating() > 5))
            throw new IllegalArgumentException("minRating moet tussen 1 en 5 liggen");

        if (request.maxRating() != null && (request.maxRating() < 1 || request.maxRating() > 5))
            throw new IllegalArgumentException("maxRating moet tussen 1 en 5 liggen");

        if (request.minRating() != null && request.maxRating() != null && request.minRating() > request.maxRating())
            throw new IllegalArgumentException("minRating mag niet groter zijn dan maxRating");
    }
}