package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.book.BookFilterRequest;
import edu.ap.gosmartlib.exceptions.NegativeValueException;
import edu.ap.gosmartlib.services.book.BookFilterValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BookFilterValidatorTest {

    @InjectMocks
    private BookFilterValidator bookFilterValidator;

    @Test
    void givenValidRequest_whenValidate_thenNoExceptionThrown() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                10, 500, 2000, 2024, 1.0, 5.0, null, null, 0, 20);

        assertDoesNotThrow(() -> bookFilterValidator.validate(request));
    }

    @Test
    void givenNullOptionalFields_whenValidate_thenNoExceptionThrown() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, 0, 10);

        assertDoesNotThrow(() -> bookFilterValidator.validate(request));
    }

    @Test
    void givenNegativePage_whenValidate_thenThrowsNegativeValueException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, -1, 10);

        assertThrows(NegativeValueException.class, () -> bookFilterValidator.validate(request));
    }

    @Test
    void givenZeroSize_whenValidate_thenThrowsNegativeValueException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, 0, 0);

        assertThrows(NegativeValueException.class, () -> bookFilterValidator.validate(request));
    }

    @Test
    void givenMinPageCountGreaterThanMaxPageCount_whenValidate_thenThrowsIllegalArgumentException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                500, 100, null, null, null, null, null, null, 0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookFilterValidator.validate(request));
        assertTrue(ex.getMessage().contains("minPageCount"));
    }

    @Test
    void givenMinPubYearGreaterThanMaxPubYear_whenValidate_thenThrowsIllegalArgumentException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, 2024, 2000, null, null, null, null, 0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookFilterValidator.validate(request));
        assertTrue(ex.getMessage().contains("minPubYear"));
    }

    @Test
    void givenMinRatingBelowOne_whenValidate_thenThrowsIllegalArgumentException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, 0.5, null, null, null, 0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookFilterValidator.validate(request));
        assertTrue(ex.getMessage().contains("minRating"));
    }

    @Test
    void givenMinRatingAboveFive_whenValidate_thenThrowsIllegalArgumentException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, 5.5, null, null, null, 0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookFilterValidator.validate(request));
        assertTrue(ex.getMessage().contains("minRating"));
    }

    @Test
    void givenMaxRatingBelowOne_whenValidate_thenThrowsIllegalArgumentException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, null, 0.5, null, null, 0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookFilterValidator.validate(request));
        assertTrue(ex.getMessage().contains("maxRating"));
    }

    @Test
    void givenMaxRatingAboveFive_whenValidate_thenThrowsIllegalArgumentException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, null, 6.0, null, null, 0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookFilterValidator.validate(request));
        assertTrue(ex.getMessage().contains("maxRating"));
    }

    @Test
    void givenMinRatingGreaterThanMaxRating_whenValidate_thenThrowsIllegalArgumentException() {
        BookFilterRequest request = new BookFilterRequest(null, null, null, null, null,
                null, null, null, null, 4.0, 2.0, null, null, 0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookFilterValidator.validate(request));
        assertTrue(ex.getMessage().contains("minRating"));
    }
}
