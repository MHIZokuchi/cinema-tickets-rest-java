package uk.gov.dwp.engineering.recruitment.booking;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TicketPricingTest {
    private final TicketPricing ticketPricing = new TicketPricing();

    @ParameterizedTest
    @CsvSource({
            "ADULT,  25.99",
            "CHILD,  17.50",
            "INFANT, 0.00"
    })

    void givenTicketType_whenPriceOf_thenPriceFromBusinessRulesReturned(final TicketType type,
                                                                        final BigDecimal expectedPrice) {
        assertEquals(expectedPrice, ticketPricing.ticketPriceOf(type));
    }

    @ParameterizedTest
    @CsvSource({
            "1,  0,  0, 25.99",
            "2,  1,  0, 69.48",
            "1,  1,  1, 43.49",
            "2,  0,  2, 51.98",
            "25, 0,  0, 649.75",
            "10, 15, 0, 522.40",
            "0,  0,  0, 0.00"
    })
    void givenOrder_whenTotalFor_thenExactTotalReturned(final long adults, final long children,
                                                        final long infants, final BigDecimal expectedTotal) {
        final TicketOrder order = new TicketOrder(adults, children, infants);

        assertEquals(expectedTotal, ticketPricing.sumTotalPrice(order));
    }
}
