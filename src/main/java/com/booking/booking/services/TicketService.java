package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.ticket.PostTicketDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.TicketStatus;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public Map<String, Object> getTicket(Long tickedId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findById(tickedId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + tickedId));
        if (ticket.getUser().getId() != user.getId())
            throw new EntityNotFoundException("You are not autorized to access other's tickets");
        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket retrieved successfully.");
        mapTicket.put("data", ticket);
        return mapTicket;
    }

    public Map<String, Object> postTicket(PostTicketDto postTicketDto) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Event event = eventRepository.findById(postTicketDto.getEventId()).orElseThrow(() -> new EntityNotFoundException("No event found with the given ID " + postTicketDto.getEventId()));
        if (user.getId() == event.getUser().getId())
            throw new EntityNotFoundException("You cannot book a ticket for your own event");
        if (event.getEventDateTime().isEqual(LocalDateTime.now()) || event.getEventDateTime().isBefore(LocalDateTime.now()))
            throw new EntityNotFoundException("The event already started or finished");
        long countTicketsConfirmed = ticketRepository.countByStatus(TicketStatus.CONFIRMED);
        if (countTicketsConfirmed == event.getTotalTickets())
            throw new EntityNotFoundException("all the tickets has been sold");
        if (event.getAvailableTickets() == 0)
            throw new EntityNotFoundException("all tickets are booked, Try again later");
        Optional<Ticket> ticket = ticketRepository.findByUserIdAndEventIdAndStatusNot(user.getId(), event.getId(), TicketStatus.CANCELED);
        if (ticket.isPresent())
            throw new EntityNotFoundException("You already purchased or booked a ticket for this event");
        Ticket newTicket = Ticket
            .builder()
            .user(user)
            .event(event)
            .status(TicketStatus.PENDING)
            .build();
        newTicket = ticketRepository.save(newTicket);
        event.setAvailableTickets(event.getAvailableTickets() - 1);
        eventRepository.save(event);
        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket added successfully.");
        mapTicket.put("data", newTicket);
        return mapTicket;
    }

    public Map<String, Object> getAllTickets(int page, int size) {
        Map<String, Object> mapTicket = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt"));
        Page<Event> tickets = ticketRepository.findAllByUserId(pageable, user.getId());
        List<Event> ticketList = tickets.stream().toList();
        mapPagination.put("currentPage", tickets.getNumber());
        mapPagination.put("pageSize", tickets.getSize());
        mapPagination.put("totalElements", tickets.getTotalElements());
        mapPagination.put("totalPages", tickets.getTotalPages());
        mapPagination.put("firstPage", tickets.isFirst());
        mapPagination.put("lastPage", tickets.isLast());
        mapTicket.put("status", "success");
        mapTicket.put("message", "Tickets retrieved successfully.");
        mapTicket.put("data", ticketList);
        mapTicket.put("pagination", mapPagination);
        return mapTicket;
    }

    public Map<String, Object> cancelTicket(Long tickedId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findById(tickedId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + tickedId));
        if (ticket.getUser().getId() != user.getId())
            throw new EntityNotFoundException("You are not autorized to cancel other's tickets");
        if (ticket.getStatus() != TicketStatus.PENDING)
            throw new EntityNotFoundException("Your ticket already canceled or confirmed");
        ticket.builder().status(TicketStatus.CANCELED);
        ticket = ticketRepository.save(ticket);
        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket canceled successfully.");
        mapTicket.put("data", ticket);
        return mapTicket;
    }
}