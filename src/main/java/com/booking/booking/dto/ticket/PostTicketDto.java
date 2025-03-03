package com.booking.booking.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PostTicketDto {
    @NotNull(message = "Event ID cannot be null")
    private Long eventId;
}
