package uk.gov.dwp.engineering.recruitment.api;

import uk.gov.dwp.engineering.recruitment.domain.Booking;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

import java.util.List;

public record BookingRequestDto(Long accountId, List<TicketRequestDto> tickets) {

    public Booking toBooking() {
        final TicketRequest[] requests = tickets == null
                ? null
                : tickets.stream()
                .map(t -> t == null ? null : t.toTicketRequest())
                .toArray(TicketRequest[]::new);
        return new Booking(accountId, requests);
    }
}

