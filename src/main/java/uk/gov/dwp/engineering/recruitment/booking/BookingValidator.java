package uk.gov.dwp.engineering.recruitment.booking;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

@Component
public class BookingValidator {
    static final  int MAX_TICKETS_PER_BOOKING = 25;

    static final String INVALID_ACCOUNT_ID = "Account id must be greater than zero";
    static final String NO_TICKETS_REQUESTED = "At least one ticket must be requested";
    static final String NULL_TICKET_REQUEST = "Ticket requests must not be null";
    static final String MISSING_TICKET_TYPE = "Each ticket request must have a ticket type";
    static final String NEGATIVE_TICKET_COUNT = "Ticket counts must not be negative";
    static final String TOO_MANY_TICKETS =
            "A maximum of %d tickets can be purchased at a time".formatted(MAX_TICKETS_PER_BOOKING);
    static final String ADULT_REQUIRED =
            "Child and infant tickets cannot be purchased without an adult ticket";
    static final String TOO_MANY_INFANTS =
            "Each infant must sit on an adult's lap, so infants cannot outnumber adults";

    //Validates the request is well formated
    public void validateRequest(final Long accountId, final  TicketRequest... ticketRequests)
    {
        if(accountId == null || accountId <= 0)
            throw new InvalidBookingException(INVALID_ACCOUNT_ID);

        if(ticketRequests == null || ticketRequests.length == 0)
            throw new InvalidBookingException(NO_TICKETS_REQUESTED);

        for (final  TicketRequest ticketRequest : ticketRequests){
            if(ticketRequest == null)
                throw new InvalidBookingException(NULL_TICKET_REQUEST);

            if(ticketRequest.type() == null)
                throw new InvalidBookingException(MISSING_TICKET_TYPE);

            if (ticketRequest.ticketCount() < 0)
                throw new InvalidBookingException(NEGATIVE_TICKET_COUNT);
        }

    }

    //Business logic
    public void validateOrder(final TicketOrder order){
        if(order.totalTickets() == 0)
            throw new InvalidBookingException(NO_TICKETS_REQUESTED);

        if(order.totalTickets() > MAX_TICKETS_PER_BOOKING)
            throw new InvalidBookingException(TOO_MANY_TICKETS);

        if(order.adults() == 0 && (order.children() >0 || order.infants() > 0))
            throw new InvalidBookingException(ADULT_REQUIRED);

        if(order.infants() > order.adults())
            throw new InvalidBookingException(TOO_MANY_INFANTS);

    }
}
