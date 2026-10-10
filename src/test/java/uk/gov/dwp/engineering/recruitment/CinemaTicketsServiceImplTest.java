package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.booking.BookingValidator;
import uk.gov.dwp.engineering.recruitment.booking.TicketPricing;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.*;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

    private static final long ACCOUNT_ID = 123L;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private CinemaTicketsServiceImpl cinemaTicketsService;

    @BeforeEach
    void initialize(){
        cinemaTicketsService = new CinemaTicketsServiceImpl(paymentService, seatReservationService,
                new BookingValidator(), new TicketPricing());
    }

    @ParameterizedTest
    @CsvSource({
            "1,  0,  0,  25.99,  1",   // one adult
            "2,  1,  0,  69.48,  3",   // family
            "1,  1,  1,  43.49,  2",   // one of each type
            "2,  0,  2,  51.98,  2",   // infants are free and unseated
            "25, 0,  0,  649.75, 25",  // maximum booking
            "13, 0,  12, 337.87, 13"   // maximum including infants
    })
    void givenValidBookingRequest_whenPurchaseTickets_thenCorrectAmountPaidAndSeatsReserved(
            final int adults, final int children, final int infants, final BigDecimal expectedCost,
            final long expectedSeats) {
        final BookingConfirmation confirmation = cinemaTicketsService.purchaseTickets(ACCOUNT_ID,
                new TicketRequest(ADULT, adults), new TicketRequest(CHILD, children),
                new TicketRequest(INFANT, infants));

        verify(paymentService).debitAccount(ACCOUNT_ID, expectedCost);
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, expectedSeats);
        assertEquals(new BookingConfirmation(ACCOUNT_ID, expectedSeats, expectedCost), confirmation);
    }

    @Test
    void givenRepeatedTicketType_whenPurchaseTickets_thenTicketRequestsCombined() {
        cinemaTicketsService.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 2),
                new TicketRequest(ADULT, 3));

        verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("129.95"));
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, 5L);
    }

    @Test
    void givenValidBooking_whenPurchaseTickets_thenPaymentTakenBeforeSeatsReserved() {
        cinemaTicketsService.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1));

        final InOrder inOrder = inOrder(paymentService, seatReservationService);
        inOrder.verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("25.99"));
        inOrder.verify(seatReservationService).reserveSeats(ACCOUNT_ID, 1L);
    }

    @ParameterizedTest
    @MethodSource("invalidBookings")
    void givenInvalidBooking_whenPurchaseTickets_thenRejectedAndNothingChargedOrReserved(
            final Long accountId, final TicketRequest[] ticketRequests) {
        assertThrows(InvalidBookingException.class,
                () -> cinemaTicketsService.purchaseTickets(accountId, ticketRequests));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    private static Stream<Arguments> invalidBookings() {
        return Stream.of(
                argumentSet("account id of zero", 0L,
                        requests(new TicketRequest(ADULT, 1))),
                argumentSet("missing account id", null,
                        requests(new TicketRequest(ADULT, 1))),
                argumentSet("missing ticket requests", ACCOUNT_ID, null),
                argumentSet("empty ticket requests", ACCOUNT_ID, requests()),
                argumentSet("negative ticket count", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 2), new TicketRequest(ADULT, -1))),
                argumentSet("zero tickets in total", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 0))),
                argumentSet("more than 25 tickets", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 26))),
                argumentSet("infants count towards 25", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 13), new TicketRequest(INFANT, 13))),
                argumentSet("child without an adult", ACCOUNT_ID,
                        requests(new TicketRequest(CHILD, 1))),
                argumentSet("infant without an adult", ACCOUNT_ID,
                        requests(new TicketRequest(INFANT, 1))),
                argumentSet("more infants than adults", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 1), new TicketRequest(INFANT, 2))));
    }

    private static TicketRequest[] requests(final TicketRequest... ticketRequests) {
        return ticketRequests;
    }

}
