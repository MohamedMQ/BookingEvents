package com.booking.booking.dto.Notifications;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueueInfosDto {
    private String message;
    private Long eventId;
    private Integer queuePos;
}
