package uk.gov.dwp.engineering.recruitment.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import org.junit.jupiter.api.function.Executable;

import java.util.stream.Stream;

public class BookingValidatorTest {

    private final BookingValidator bookingValidator = new BookingValidator();

    @Test
    void givenWellFormedRequest_whenValidateRequest_thenNoExceptionThrown() {
        assertDoesNotThrow(() -> bookingValidator.validateRequest(1L,
                new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 0)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void givenAccountIdNotGreaterThanZero_whenValidateRequest_thenRejected(final Long accountId) {
        assertRejected(BookingValidator.INVALID_ACCOUNT_ID,
                () -> bookingValidator.validateRequest(accountId, new TicketRequest(ADULT, 1)));
    }

    @Test
    void givenNullTicketRequests_whenValidateRequest_thenRejected() {
        assertRejected(BookingValidator.NO_TICKETS_REQUESTED,
                () -> bookingValidator.validateRequest(1L, (TicketRequest[]) null));
    }

    @Test
    void givenEmptyTicketRequests_whenValidateRequest_thenRejected() {
        assertRejected(BookingValidator.NO_TICKETS_REQUESTED,
                () -> bookingValidator.validateRequest(1L));
    }

    @Test
    void givenNullTicketRequest_whenValidateRequest_thenRejected() {
        assertRejected(BookingValidator.NULL_TICKET_REQUEST,
                () -> bookingValidator.validateRequest(1L, new TicketRequest(ADULT, 1), null));
    }

    @Test
    void givenTicketRequestWithoutTicketType_whenValidateRequest_thenRejected() {
        assertRejected(BookingValidator.MISSING_TICKET_TYPE,
                () -> bookingValidator.validateRequest(1L, new TicketRequest(null, 1)));
    }

    @Test
    void givenNegativeTicketCount_whenValidateRequest_thenRejected() {
        assertRejected(BookingValidator.NEGATIVE_TICKET_COUNT,
                () -> bookingValidator.validateRequest(1L, new TicketRequest(ADULT, -1)));
    }

    @ParameterizedTest
    @MethodSource("validOrders")
    void givenOrderWithinRules_whenValidateOrder_thenNoExceptionThrown(final TicketOrder order) {
        assertDoesNotThrow(() -> bookingValidator.validateOrder(order));
    }

    @Test
    void givenNoTickets_whenValidateOrder_thenRejected() {
        assertRejected(BookingValidator.NO_TICKETS_REQUESTED,
                () -> bookingValidator.validateOrder(new TicketOrder(0, 0, 0)));
    }

    @ParameterizedTest
    @MethodSource("ordersOverMaximum")
    void givenTicketRequestMoreThanMaxAllowed_whenValidateOrder_thenRejected(final TicketOrder order) {
        assertRejected(BookingValidator.TOO_MANY_TICKETS,
                () -> bookingValidator.validateOrder(order));
    }

    @ParameterizedTest
    @MethodSource("ordersWithoutAdult")
    void givenChildOrInfantWithoutAdult_whenValidateOrder_thenRejected(final TicketOrder order) {
        assertRejected(BookingValidator.ADULT_REQUIRED,
                () -> bookingValidator.validateOrder(order));
    }

    @ParameterizedTest
    @MethodSource("ordersWithTooManyInfants")
    void givenMoreInfantsThanAdults_whenValidateOrder_thenRejected(final TicketOrder order) {
        assertRejected(BookingValidator.TOO_MANY_INFANTS,
                () -> bookingValidator.validateOrder(order));
    }

    static Stream<TicketOrder> validOrders() {
        return Stream.of(
                new TicketOrder(1, 0, 0),    // smallest valid booking
                new TicketOrder(1, 1, 1),    // one of each type
                new TicketOrder(2, 0, 2),    // infants equal to adults
                new TicketOrder(25, 0, 0),   // exactly the maximum
                new TicketOrder(13, 0, 12),  // maximum including infants
                new TicketOrder(1, 24, 0)    // one adult with many children
        );
    }

    static Stream<TicketOrder> ordersOverMaximum() {
        return Stream.of(
                new TicketOrder(26, 0, 0),                    // one over the maximum
                new TicketOrder(13, 0, 13),                   // infants count towards the maximum
                new TicketOrder(24, 1, 1)                     // mixed types over the maximum
        );
    }

    static Stream<TicketOrder> ordersWithoutAdult() {
        return Stream.of(
                new TicketOrder(0, 1, 0),  // child only
                new TicketOrder(0, 0, 1),  // infant only
                new TicketOrder(0, 2, 1)   // children and infant, no adult
        );
    }

    static Stream<TicketOrder> ordersWithTooManyInfants() {
        return Stream.of(
                new TicketOrder(1, 0, 2),  // one more infant than adults
                new TicketOrder(2, 3, 3)   // children do not provide laps
        );
    }

    // ---------- helper ----------
    private static void assertRejected(final String expectedMessage, final Executable action) {
        final InvalidBookingException exception = assertThrows(InvalidBookingException.class, action);
        assertEquals(expectedMessage, exception.getMessage());
    }
}
