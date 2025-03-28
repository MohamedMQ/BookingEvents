package com.booking.booking.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.Notifications.QueueInfosDto;
import com.booking.booking.dto.Notifications.TicketInfosDto;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationService {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void sendTicketNumUpdate(TicketInfosDto eventInfosDto) {
        log.info("sending event infos with payload {}", eventInfosDto);
        simpMessagingTemplate.convertAndSend("/topic", eventInfosDto);
    }

    public void sendQueuePosUpdate(
        String username,
        QueueInfosDto queueInfosDto) {
            log.info("sending queue infos with payload {}", queueInfosDto);
            simpMessagingTemplate.convertAndSendToUser(username, "/topic", queueInfosDto);
    }
}
