package uk.gov.dwp.engineering.recruitment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dwp.engineering.recruitment.api.BookingRequestDto;
import uk.gov.dwp.engineering.recruitment.domain.Booking;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;

@RestController
@RequestMapping("/cinema")
public class CinemaTicketsController {

  /**
   * The Ticket Booking Service.
   */
  private final CinemaTicketsService cinemaTicketsService;

  public CinemaTicketsController(final CinemaTicketsService cinemaTicketsService) {
    this.cinemaTicketsService = cinemaTicketsService;
  }

  /**
   * Make booking endpoint handler.
   *
   * @return ResponseEntity containing the result of the booking.
   */
  @PostMapping(value = "/bookings", consumes = "application/json", produces = "application/json")
  public ResponseEntity<BookingConfirmation> makeBooking(@RequestBody final BookingRequestDto request) {
    final Booking booking = request.toBooking();
    return new ResponseEntity<>(cinemaTicketsService.purchaseTickets(booking.accountId(),
            booking.ticketRequests()), HttpStatus.CREATED);
  }

}
