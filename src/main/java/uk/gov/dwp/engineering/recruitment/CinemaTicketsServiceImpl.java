package uk.gov.dwp.engineering.recruitment;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.booking.BookingValidator;
import uk.gov.dwp.engineering.recruitment.booking.TicketOrder;
import uk.gov.dwp.engineering.recruitment.booking.TicketPricing;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;
  private final BookingValidator bookingValidator;
  private final TicketPricing ticketPricing;

  public CinemaTicketsServiceImpl(PaymentService paymentService,
      SeatReservationService seatReservationService,
      BookingValidator bookingValidator, TicketPricing ticketPricing) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
    this.bookingValidator = bookingValidator;
    this.ticketPricing = ticketPricing;
  }

  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
      final TicketRequest... ticketRequests)
      throws InvalidBookingException {
    bookingValidator.validateRequest(accountId, ticketRequests);
    final TicketOrder ticketOrder = TicketOrder.from(ticketRequests);
    bookingValidator.validateOrder(ticketOrder);

    final BigDecimal totalCost = ticketPricing.sumTotalPrice(ticketOrder);
    final long seatCount = ticketOrder.seatsRequired();

    paymentService.debitAccount( accountId, totalCost);
    seatReservationService.reserveSeats(accountId, seatCount);

    return new BookingConfirmation(accountId, seatCount, totalCost);
  }

}
