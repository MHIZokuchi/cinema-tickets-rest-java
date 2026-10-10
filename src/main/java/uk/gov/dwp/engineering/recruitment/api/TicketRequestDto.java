package uk.gov.dwp.engineering.recruitment.api;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

public record TicketRequestDto(TicketType type, long count) {

    public TicketRequest toTicketRequest() {
        return new TicketRequest(type, (int) count);
    }
}
