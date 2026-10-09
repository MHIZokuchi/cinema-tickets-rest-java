package uk.gov.dwp.engineering.recruitment.booking;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.util.Arrays;

import static uk.gov.dwp.engineering.recruitment.domain.TicketType.*;

public record TicketOrder(long adults, long children, long infants) {

    public static TicketOrder from(final TicketRequest... ticketRequests) {
        return new TicketOrder(
                sumOf(ADULT, ticketRequests),
                sumOf(CHILD, ticketRequests),
                sumOf(INFANT, ticketRequests)
        );
    }

    public long countOf(final TicketType ticketType) {
        return switch (ticketType) {
            case ADULT -> adults;
            case CHILD -> children;
            case INFANT -> infants;
        };
    }

    public long totalTickets() {
        return adults + children + infants;
    }

    public long seatsRequired() {
        return adults + children;
    }

    private static long sumOf(final TicketType ticketType, final  TicketRequest ... ticketRequests){
        return Arrays.stream(ticketRequests)
                .filter(ticketRequest -> ticketRequest.type() == ticketType)
                .mapToLong(TicketRequest::ticketCount)
                .sum();
    }
}
