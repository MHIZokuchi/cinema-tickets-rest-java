package uk.gov.dwp.engineering.recruitment.api;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.Booking;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

public class BookingRequestDtoTest {

    @Test
    void givenValidBookingRequest_whenToBooking_thenAccountIdAndTicketsMappedInOrder() {
        final BookingRequestDto bookingRequestDto = new BookingRequestDto(42L,
                List.of(new TicketRequestDto(ADULT, 2), new TicketRequestDto(INFANT, 1)));

        final Booking booking = bookingRequestDto.toBooking();

        assertAll(
                () -> assertEquals(42L, booking.accountId()),
                () -> assertArrayEquals(
                        new TicketRequest[]{new TicketRequest(ADULT, 2), new TicketRequest(INFANT, 1)},
                        booking.ticketRequests()));
    }

    @Test
    void givenNullEmptyOrNullEntryTickets_whenToBooking_thenMappedAsIs() {
        final Booking noTickets = new BookingRequestDto(1L, null).toBooking();
        final Booking emptyTickets = new BookingRequestDto(1L, List.of()).toBooking();
        final Booking nullTicket = new BookingRequestDto(1L,
                Arrays.asList(new TicketRequestDto(ADULT, 1), null)).toBooking();

        assertAll(
                () -> assertNull(noTickets.ticketRequests()),
                () -> assertArrayEquals(new TicketRequest[0], emptyTickets.ticketRequests()),
                () -> assertArrayEquals(new TicketRequest[]{new TicketRequest(ADULT, 1), null},
                        nullTicket.ticketRequests()));
    }
}
