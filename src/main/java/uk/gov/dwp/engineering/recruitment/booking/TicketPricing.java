package uk.gov.dwp.engineering.recruitment.booking;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.math.BigDecimal;

@Component
public class TicketPricing
{
    static final BigDecimal ADULT_PRICE = new BigDecimal("25.99");
    static final BigDecimal CHILD_PRICE = new BigDecimal("17.50");
    static final BigDecimal INFANT_PRICE = new BigDecimal("00.00");

    public BigDecimal ticketPriceOf(final TicketType ticketType){
        return switch (ticketType){
            case ADULT -> ADULT_PRICE;
            case CHILD -> CHILD_PRICE;
            case INFANT -> INFANT_PRICE;
        };
    }

    public BigDecimal sumTotalPrice(final TicketOrder ticketOrder){
        BigDecimal total = BigDecimal.ZERO;

        for (final TicketType type : TicketType.values()){
            final BigDecimal ticketTypePrice = ticketPriceOf(type).multiply(BigDecimal.valueOf(ticketOrder.countOf(type)));
            total = total.add(ticketTypePrice);
        }
        return total;
    }
}
