package com.booking.booking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.booking.booking.models.Event;
import com.booking.booking.models.Ticket;
import com.booking.booking.utils.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findById(Long id);
    long countByStatus(TicketStatus confirmed);
    Optional<Ticket> findByUserIdAndEventIdAndStatusNot(int userId, Long eventId, TicketStatus ticketStatus);
    Page<Event> findAllByUserId(Pageable pageable,int userId);
    List<Ticket> findByStatus(TicketStatus ticketStatus);
}
