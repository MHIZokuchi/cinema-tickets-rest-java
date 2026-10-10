package uk.gov.dwp.engineering.recruitment.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class RestExceptionHandlerTest {

    private final RestExceptionHandler restExceptionHandler = new RestExceptionHandler();

    @Test
    void givenInvalidBookingException_whenHandled_thenBadRequestDetailedProblemIsReturned() {
        final ProblemDetail problemDetail = restExceptionHandler.handleInvalidBookingException(
                new InvalidBookingException("A useful message")
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals(RestExceptionHandler.INVALID_BOOKING_TITLE, problemDetail.getTitle());
        assertEquals("A useful message", problemDetail.getDetail());
    }
}
