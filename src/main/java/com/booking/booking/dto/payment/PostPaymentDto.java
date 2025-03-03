package com.booking.booking.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostPaymentDto {
    @NotBlank(message = "Stripe token must not be blank")
    private String stripeToken;

    @NotNull(message = "Ticket id must not be null")
    private Long ticketId;
}