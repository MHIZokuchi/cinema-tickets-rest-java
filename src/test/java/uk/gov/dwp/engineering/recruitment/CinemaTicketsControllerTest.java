package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.dwp.engineering.recruitment.booking.BookingValidator;
import uk.gov.dwp.engineering.recruitment.booking.TicketPricing;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.dwp.engineering.recruitment.booking.BookingValidator.*;
import static uk.gov.dwp.engineering.recruitment.exception.RestExceptionHandler.INVALID_BOOKING_TITLE;

@WebMvcTest(CinemaTicketsController.class)
@Import({CinemaTicketsServiceImpl.class, BookingValidator.class, TicketPricing.class})
class CinemaTicketsControllerTest {

    private static final String BOOKINGS_URL = "/cinema/bookings";

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private SeatReservationService seatReservationService;

    @Test
    void givenValidBookingRequest_whenPostBooking_thenCreatedWithConfirmation() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                                  {
                                                        "accountId": 1234,
                                                        "tickets": [
                                                          { "type": "ADULT", "count": 2 },
                                                          { "type": "CHILD", "count": 1 },
                                                          { "type": "INFANT", "count": 1 }
                                                        ]
                                                  }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(1234))
                .andExpect(jsonPath("$.seatCount").value(3))
                .andExpect(jsonPath("$.totalCost").value(69.48));


        verify(paymentService).debitAccount(1234L, new BigDecimal("69.48"));
        verify(seatReservationService).reserveSeats(1234L, 3L);
    }

    @ParameterizedTest
    @MethodSource("bookingsBreakingARule")
    void givenBookingBreakingARule_whenPostBooking_thenBadRequestWithProblemDetail(
            final String requestBody, final String expectedDetail) throws Exception {
        postBooking(requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value(INVALID_BOOKING_TITLE))
                .andExpect(jsonPath("$.detail").value(expectedDetail));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenIncorrectHttpMethodRequest_whenBookingsCalled_thenMethodNotAllowed() throws Exception {
        mockMvc.perform(get(BOOKINGS_URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));


        verifyNoInteractions(paymentService, seatReservationService);
    }

    private static Stream<Arguments> bookingsBreakingARule() {
        return Stream.of(
                argumentSet("invalid account id",
                        json("{'accountId': -5, 'tickets': [{'type': 'ADULT', 'count': 1}]}"),
                        INVALID_ACCOUNT_ID),
                argumentSet("missing account id",
                        json("{'tickets': [{'type': 'ADULT', 'count': 1}]}"),
                        INVALID_ACCOUNT_ID),
                argumentSet("missing tickets",
                        json("{'accountId': 1234}"),
                        NO_TICKETS_REQUESTED),
                argumentSet("empty tickets",
                        json("{'accountId': 1234, 'tickets': []}"),
                        NO_TICKETS_REQUESTED),
                argumentSet("null ticket request",
                        json("{'accountId': 1234, 'tickets': [null]}"),
                        NULL_TICKET_REQUEST),
                argumentSet("ticket request without a type",
                        json("{'accountId': 1234, 'tickets': [{'count': 1}]}"),
                        MISSING_TICKET_TYPE),
                argumentSet("negative ticket count",
                        json("{'accountId': 1234, 'tickets': [{'type': 'ADULT', 'count': -1}]}"),
                        NEGATIVE_TICKET_COUNT),
                argumentSet("zero tickets in total",
                        json("{'accountId': 1234, 'tickets': [{'type': 'ADULT', 'count': 0}]}"),
                        NO_TICKETS_REQUESTED),
                argumentSet("more than 25 tickets",
                        json("{'accountId': 1234, 'tickets': [{'type': 'ADULT', 'count': 26}]}"),
                        TOO_MANY_TICKETS),
                argumentSet("child without an adult",
                        json("{'accountId': 1234, 'tickets': [{'type': 'CHILD', 'count': 1}]}"),
                        ADULT_REQUIRED),
                argumentSet("infant without an adult",
                        json("{'accountId': 1234, 'tickets': [{'type': 'INFANT', 'count': 1}]}"),
                        ADULT_REQUIRED),
                argumentSet("more infants than adults",
                        json("{'accountId': 1234, 'tickets': [{'type': 'ADULT', 'count': 1},"
                                + " {'type': 'INFANT', 'count': 2}]}"),
                        TOO_MANY_INFANTS));
    }

    private ResultActions postBooking(final String requestBody) throws Exception {
        return mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private static String json(final String singleQuotedJson) {
        return singleQuotedJson.replace('\'', '"');
    }
}
