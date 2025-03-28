package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.booking.dto.ticket.PostTicketDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.Payment;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.PaymentRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.PaymentStatus;
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
    private final PaymentRepository paymentRepository;
    private final TicketQueueService ticketQueueService;

    // public Map<String, Object> getProtectedTickets(int page, int size) {
    //     Map<String, Object> mapTicket = new HashMap<>();
    //     Map<String, Object> mapPagination = new HashMap<>();
    //     User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //     Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt"));
    //     Page<Event> tickets = ticketRepository.findAllByUserId(pageable, user.getId());
    //     List<Event> ticketList = tickets.stream().toList();
    //     mapPagination.put("currentPage", tickets.getNumber());
    //     mapPagination.put("pageSize", tickets.getSize());
    //     mapPagination.put("totalElements", tickets.getTotalElements());
    //     mapPagination.put("totalPages", tickets.getTotalPages());
    //     mapPagination.put("firstPage", tickets.isFirst());
    //     mapPagination.put("lastPage", tickets.isLast());
    //     mapTicket.put("status", "success");
    //     mapTicket.put("message", "Tickets retrieved successfully.");
    //     mapTicket.put("data", ticketList);
    //     mapTicket.put("pagination", mapPagination);
    //     return mapTicket;
    // }

    // public Map<String, Object> getProtectedTicket(Long tickedId) {
    //     User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //     Ticket ticket = ticketRepository.findById(tickedId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + tickedId));
    //     if (ticket.getUser().getId() != user.getId())
    //         throw new EntityNotFoundException("You are not autorized to access other's tickets");
    //     Map<String, Object> mapTicket = new HashMap<>();
    //     mapTicket.put("status", "success");
    //     mapTicket.put("message", "Ticket retrieved successfully.");
    //     mapTicket.put("data", ticket);
    //     return mapTicket;
    // }

    @Transactional
    public Map<String, Object> postProtectedTicket(PostTicketDto postTicketDto) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Event event = eventRepository.findById(postTicketDto.getEventId()).orElseThrow(() -> new EntityNotFoundException("No event found with the given ID " + postTicketDto.getEventId()));
        if (user.getId() == event.getUser().getId())
            throw new EntityNotFoundException("You cannot book a ticket for your own event");
        if (event.getEventDateTime().isEqual(LocalDateTime.now()) || event.getEventDateTime().isBefore(LocalDateTime.now()))
            throw new EntityNotFoundException("The event already started or finished");
        // long countTicketsConfirmed = ticketRepository.countByStatus(TicketStatus.CONFIRMED);
        if (event.getAvailableTickets() == 0)
            throw new EntityNotFoundException("all the tickets has been sold");
        Optional<Ticket> ticket = ticketRepository.findByUserIdAndEventIdAndStatusNotIn(
            user.getId(), 
            event.getId(), 
            List.of(TicketStatus.CANCELED, TicketStatus.QUEUED)
        );
        if (ticket.isPresent())
            throw new EntityNotFoundException("You already purchased or booked a ticket for this event");
        System.err.println(event.getTicketNumber());
        if (event.getTicketNumber() == 0) {
            System.err.println("FDKSHAFKJAHFKJDFKLJAF=DSF==DS=F-DS=F=D-F=-D=F-ASDF-=DS");
            Optional<Ticket> queuedTicket = ticketRepository.findByUserIdAndEventIdAndStatus(user.getId(), event.getId(), TicketStatus.QUEUED);
            if (queuedTicket.isPresent())
                throw new EntityNotFoundException("You'are already been added to the queue");
            Ticket ticketToSend = ticketQueueService.addToEventQueue(user, event);
            Map<String, Object> mapTicket = new HashMap<>();
            mapTicket.put("status", "success");
            mapTicket.put("message", "You've been added to the queue successfully");
            mapTicket.put("data", ticketToSend);
            return mapTicket;
        }
        event.setTicketNumber(event.getTicketNumber() - 1);
        eventRepository.save(event);
        Ticket newTicket = Ticket
            .builder()
            .user(user)
            .event(event)
            .status(TicketStatus.PENDING)
            .queueNum(-1)
            .build();
        newTicket = ticketRepository.save(newTicket);
        Payment newPayment = Payment
            .builder()
            .ticket(newTicket)
            .sessionId("")
            .status(PaymentStatus.PENDING)
            .amount(event.getPrice())
            .build();
        paymentRepository.save(newPayment);
        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket added successfully.");
        mapTicket.put("data", newTicket);
        return mapTicket;
    }

    @Transactional
    public Map<String, Object> cancelProtectedTicket(Long tickedId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findById(tickedId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + tickedId));
        Event event = eventRepository.findById(ticket.getEvent().getId()).orElseThrow(() -> new EntityNotFoundException("No event found with this ID " + ticket.getEvent().getId()));
        if (ticket.getUser().getId() != user.getId())
        throw new EntityNotFoundException("You are not autorized to cancel other's tickets");
        if (ticket.getStatus() != TicketStatus.PENDING)
            throw new EntityNotFoundException("Your ticket already canceled or confirmed");
        if (ticket.getEvent().getEventDateTime().isEqual(LocalDateTime.now()) || ticket.getEvent().getEventDateTime().isBefore(LocalDateTime.now()))
            throw new EntityNotFoundException("The event already started or finished");
        ticket.setStatus(TicketStatus.CANCELED);
        ticketRepository.save(ticket);
        event.setTicketNumber(event.getTicketNumber() + 1);
        eventRepository.save(event);
        System.err.println(event.getTicketNumber());

        LinkedList<Long> queueTicketList = ticketQueueService.getEventQueue(ticket.getEvent().getId());
        if (!queueTicketList.isEmpty()) {
            Long queuedTicketId = queueTicketList.removeFirst();
            Ticket queuedTicket = ticketRepository.findById(queuedTicketId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + queuedTicketId));
            queuedTicket.setStatus(TicketStatus.PENDING);
            queuedTicket.setQueueNum(-1);
            ticketRepository.save(queuedTicket);
            Payment newPayment = Payment
                .builder()
                .ticket(queuedTicket)
                .sessionId("")
                .status(PaymentStatus.PENDING)
                .amount(queuedTicket.getEvent().getPrice())
                .build();
            paymentRepository.save(newPayment);
            
            // THIS SHOULD BE ASYNC TO NOT BLOCK THE RESPONSE 
            queueTicketList.forEach(tId -> {
                // WILL BE ADDED LATER ON
            });

        }

        System.err.println(ticket);
        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket canceled successfully.");
        mapTicket.put("data", ticket);
        return mapTicket;
    }
}