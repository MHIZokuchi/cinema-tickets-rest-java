package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.dwp.engineering.recruitment.booking.BookingValidator;
import uk.gov.dwp.engineering.recruitment.booking.TicketPricing;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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


}
