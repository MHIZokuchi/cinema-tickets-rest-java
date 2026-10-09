package uk.gov.dwp.engineering.recruitment.booking;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.*;

public class TicketOrderTest {
    @Test
    void givenOneTicketRequestPerType_whenFrom_thenCountsTotalAndSeatsCorrectly() {
        final TicketOrder order = TicketOrder.from(
                new TicketRequest(ADULT, 2),
                new TicketRequest(CHILD, 1),
                new TicketRequest(INFANT, 1)
        );

        assertEquals(new TicketOrder(2,1,1), order);
        assertEquals(4, order.totalTickets());
        assertEquals(3, order.seatsRequired());
    }

    @Test
    void givenRepeatedTicketType_whenFrom_thenSumTogether() {
        final TicketOrder order = TicketOrder.from(
                new TicketRequest(ADULT, 2),
                new TicketRequest(ADULT, 4)
        );

        assertEquals(6, order.adults());
    }

    @Test
    void givenInfants_whenSeatsRequired_thenInfantsNotSeated() {
        final TicketOrder order = new TicketOrder(2,0,2);

        assertEquals(2, order.seatsRequired());
        assertEquals(4, order.totalTickets());
    }

    @Test
    void givenOrder_whenCountOd_thenMatchingCountRequired() {
        final TicketOrder order = new TicketOrder(3,2,1);

        assertEquals(3, order.countOf(ADULT));
        assertEquals(2, order.countOf(CHILD));
        assertEquals(1, order.countOf(INFANT));
    }
}
