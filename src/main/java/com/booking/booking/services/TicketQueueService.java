package com.booking.booking.services;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.springframework.stereotype.Service;

import com.booking.booking.models.Event;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.TicketStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Service
public class TicketQueueService {
    private final Map<Long, LinkedList<Long>> eventQueues = new HashMap<>();
    private final TicketRepository ticketRepository;

    public Ticket addToEventQueue(User user, Event event) {
        Integer pos = 1;
        Ticket ticketToSend;
        if (eventQueues.get(event.getId()) == null) {
            LinkedList<Long> newEventQueue = new LinkedList<>();
            Ticket newTicket = Ticket
                .builder()
                .user(user)
                .event(event)
                .status(TicketStatus.QUEUED)
                .queueNum(1)
                .qrCodeUrl("")
                .build();
            ticketRepository.save(newTicket);
            newEventQueue.add(newTicket.getId());
            eventQueues.put(event.getId(), newEventQueue);
            ticketToSend = newTicket;
        } else {
            LinkedList<Long> eventQueue = eventQueues.get(event.getId());
            Ticket newTicket = Ticket
                .builder()
                .user(user)
                .event(event)
                .status(TicketStatus.QUEUED)
                .queueNum(-1)
                .qrCodeUrl("")
                .build();
            ticketRepository.save(newTicket);
            eventQueue.add(newTicket.getId());
            pos = eventQueue.indexOf(newTicket.getId()) + 1;
            newTicket.setQueueNum(pos);
            ticketRepository.save(newTicket);
            ticketToSend = newTicket;
        }
        return ticketToSend;
    }

    public LinkedList<Long> getEventQueue(Long eventId) {
        if (eventQueues.get(eventId) == null)
            return new LinkedList<>();
        return eventQueues.get(eventId);
    }
}
