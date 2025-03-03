package com.booking.booking.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.booking.booking.models.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    Page<Event> findAll(Pageable pageable);
    Page<Event> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrLocationContainingIgnoreCase(
        Pageable pageable, String name, String description, String location);
    Optional<Event> findById(Long eventId);
}