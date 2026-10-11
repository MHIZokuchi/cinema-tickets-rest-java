# Cinema Tickets Code Test

## Objective

To assess your ability to build a simple API.

The scenario and requirements will be provided separately.

## Contents


- [Requirements](#running-the-service)
- [Using the API](#using-the-api)
- [Business rules](#business-rules)
- [Assumptions and interpretations](#assumptions-and-interpretations)
- [Design](#design)
- [Error handling](#error-handling)
- [Testing](#testing)
- [Known limitations and next steps](#known-limitations-and-next-steps)


## Requirements


**Requirements:** Java 21 or later, and Maven 3.9 or later.

## Using the API


The API is described in [`src/main/resources/cinema-tickets.yaml`](src/main/resources/cinema-tickets.yaml)
(OpenAPI 3.1). It has one endpoint: `POST /cinema/bookings`.


### A successful booking


```bash
curl -i -X POST http://localhost:8080/cinema/bookings \
  -H "Content-Type: application/json" \
  -d '{
        "accountId": 1234,
        "tickets": [
          { "type": "ADULT",  "count": 2 },
          { "type": "CHILD",  "count": 1 },
          { "type": "INFANT", "count": 1 }
        ]
      }'
```


```http
HTTP/1.1 201 Created
Content-Type: application/json


{ "accountId": 1234, "seatCount": 3, "totalCost": 69.48 }
```


Two adults and one child need 3 seats; the infant sits on an adult's lap. The cost is
2 × £25.99 + 1 × £17.50 + 1 × £0 = £69.48.


### A rejected booking


```bash
curl -i -X POST http://localhost:8080/cinema/bookings \
  -H "Content-Type: application/json" \
  -d '{ "accountId": 1234, "tickets": [ { "type": "CHILD", "count": 1 } ] }'
```


```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json


{
  "type": "about:blank",
  "title": "Invalid booking",
  "status": 400,
  "detail": "Child and infant tickets cannot be purchased without an adult ticket",
  "instance": "/cinema/bookings"
}
```


Errors follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457). The `detail`
field says which rule was broken.


## Business rules


| Rule  |
|---|
| Ticket prices are ADULT £25.99, CHILD £17.50, INFANT £0  |
| Infants are not allocated a seat  |
| At most 25 tickets per purchase  |
| CHILD and INFANT tickets need at least one ADULT ticket |
| Account ids must be greater than zero  |
| No more infants than adults, because an infant can only seat on an adult's lap |
| Ticket counts must not be negative|
| A booking must contain at least one ticket |
| Every ticket request must have a type and must not be null |


## Assumptions and interpretations


The brief allows these assumptions, and they are relied on throughout:


- All accounts with an id greater than zero are valid and have enough funds.
- `PaymentService` and `SeatReservationService` are external providers with no defects; payments
  always go through and seats are always reserved once requested.


The following are my own interpretations where the brief was ambiguous or silent.


| # | Question | Decision | Reasoning |
|---|---|---|---|
| A1 | Do infants count towards the 25-ticket limit? | **Yes.** All tickets count, including infants. | The rule says "tickets", and an infant is issued a (free) ticket. |
| A2 | Can one adult hold more than one infant? | **No.** A booking is rejected if infants outnumber adults. | Infants sit "on an ADULT lap"; one lap per infant. |
| A3 | What if the same ticket type appears more than once? | **Counts are added together**, e.g. ADULT 2 + ADULT 3 = 5 adults, charged as one payment. | Same types are grouped together |
| A4 | Negative counts | **Rejected.** | It is not possible to buy tickets that are negative in number. |
| A5 | Null or empty ticket lists, null entries, missing ticket type | **Rejected** with a specific message. | Malformed requests. |
| A6 | Why was the request body for `POST /cinema/bookings` changed? | **The request now matches the OpenAPI spec.** The controller originally read the body straight into the domain `Booking` record, whose fields (`ticketRequests`, `ticketCount`) don't match the spec (`tickets`, `count`). New request DTOs in the `api` package follow the spec, and the controller maps them to the domain records. | The OpenAPI spec (`cinema-tickets.yml`) is the published contract that clients rely on, so the API must follow it. The domain records couldn't be renamed because the domain package must not be modified. |
| A7 | What is the flow of a booking request? | **Validate the whole booking first, then take payment, then reserve seats.** | If a booking is invalid, it is rejected before payment or booking happens, so the customer is never charged and no seats are reserved. Taking payment before reserving means seats are never held for a booking that hasn't been paid for. |
| A8 | Does the service check whether the payment and seat reservation succeeded? | **No.** The responses from `PaymentService` and `SeatReservationService` are not checked. | The brief states that both services always succeed once a request is made. A real system would need to handle failures; see [Known limitations and next steps](#known-limitations-and-next-steps). |
| A9 | Money type | **`BigDecimal`** with two decimal places throughout. | Exact currency arithmetic; `double` cannot represent most decimal amounts exactly. |


## Design


### Request flow


```
POST /cinema/bookings (JSON)
  → CinemaTicketsController        maps BookingRequestDto → TicketRequest[]
  → CinemaTicketsServiceImpl
       1. BookingValidator.validateRequest   is the input well formed?
       2. TicketOrder.from                   add up the counts per ticket type
       3. BookingValidator.validateOrder     do the totals follow the business rules?
       4. TicketPricing.sumTotalPrice             total cost
       5. TicketOrder.seatsRequired          seats (infants excluded)
       6. PaymentService.debitAccount
       7. SeatReservationService.reserveSeats
  ← 201 Created + BookingConfirmation
```

Any broken rule throws `InvalidBookingException` before step 6, and `RestExceptionHandler` turns it
into a 400 Problem Detail.


### Packages


| Package | Contents |
|---|---|
| (root) | `CinemaTicketsController`, `CinemaTicketsService` (unchanged), `CinemaTicketsServiceImpl` |
| `api` | `BookingRequestDto`, `TicketRequestDto`: the HTTP request shape from the OpenAPI spec |
| `booking` | `BookingValidator`, `TicketOrder`, `TicketPricing`: the booking rules and calculations |
| `domain` | Unchanged, apart from `BookingConfirmation` (see below) |
| `exception` | `InvalidBookingException`, `RestExceptionHandler` |
| `thirdparty` | Unchanged |



### Decisions

- **Validation happens in two steps.** Individual ticket requests are checked first (nulls, negatives), then the totals (maximum, adult present).
- **No `default` in the price switch.** Adding a new `TicketType` without a price won't compile, so it can't slip through as a free ticket.
- **`BookingConfirmation` was extended, not changed.** I added `seatCount` and `totalCost` because the spec needs them, and kept the old constructor so existing code still compiles.
- **Invalid bookings return 400, not 501.** The starter handler returned 501, but a broken rule is a client error.

## Error handling


Every error response is an RFC 9457 Problem Detail with content type `application/problem+json`,
and no error ever reaches the payment or reservation services.


## Testing


| Test class | Type | What it covers |
|---|---|---|
| `TicketOrderTest` | Unit | Adding up counts, repeated types, zero counts, seats exclude infants, no overflow |
| `TicketPricingTest` | Unit | Each price, and totals for a range of bookings including the maximum |
| `BookingValidatorTest` | Unit | Every rule and its message, with both sides of each boundary (e.g. 25 accepted, 26 rejected) |
| `BookingRequestDtoTest` | Unit | Mapping the request DTOs to domain records, including nulls |
| `RestExceptionHandlerTest` | Unit | Invalid bookings become a 400 Problem Detail |
| `CinemaTicketsServiceImplTest` | Unit, with Mockito | Correct amount paid and seats reserved; payment before reservation; invalid bookings never reach payment or reservation |
| `CinemaTicketsControllerTest` | Web slice (`@WebMvcTest` + `MockMvc`) | Real HTTP requests and JSON through the real controller, service and rules, with only the two external services mocked. Covers every business rule, the 400/415/405 responses, and the exact error messages clients see |


## Known limitations and next steps

- **Payment and reservation failures.** The brief guarantees both external calls succeed. A real
  system would need to check their responses, and to refund the payment if the seat reservation
  failed after payment was taken.
- **Idempotency.** Retrying the same request makes a second booking. An idempotency key would
  prevent double charging.
- **Configuration.** Prices and the 25-ticket limit are constants in code. They could be moved to
  configuration if they change often.






