package com.booking.booking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.booking.booking.models.Event;
import com.booking.booking.utils.EventStatus;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAll(Pageable pageable);
    Page<Event> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrLocationContainingIgnoreCase(
        Pageable pageable, String name, String description, String location);
    Optional<Event> findById(Long eventId);
    Optional<Event> findByIdAndUserId(Long eventId, Integer userId);
    @Query("""
        SELECT DISTINCT e FROM Event e
        LEFT JOIN e.tickets t
        ON t.event.id = e.id
        AND t.user.id = :userId
        AND (t.status = 'PENDING' OR t.status = 'CONFIRMED')
    """)
    Page<Event> findAllEventsWithUserTickets(Pageable pageable, int userId);
    Page<Event> findByUserId(Pageable pageable, int id);
    Page<Event> findByEventStatusNot(Pageable pageable, EventStatus canceled);
    Optional<Event> findByIdAndEventStatusNot(Long eventId, EventStatus canceled);
    Optional<Event> findByIdAndUserIdAndEventStatus(Long eventId, int id, EventStatus confirmed);
    Optional<Event> findByIdAndEventStatusNotIn(Long eventId, List<EventStatus> eventStatusList);
    Page<Event> findByEventStatusNotIn(Pageable pageable, List<EventStatus> eventStatusList);
}