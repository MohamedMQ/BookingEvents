package com.booking.booking.dto.user;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatusResponseDto {
    private List<String> currentlyDue;
    private String disabledReason;
    private String acceptPayments;
    private String recievePayout;
}
