package com.booking.booking.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.event.PostEventDto;
import com.booking.booking.services.EventService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    @GetMapping
    ResponseEntity<Map<String, Object>> events(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        Map<String, Object> mapEvent = eventService.getAllEvents(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(mapEvent);
    }

    @GetMapping("/{eventId}")
    ResponseEntity<Map<String, Object>> event(@PathVariable("eventId") Integer eventId) {
        Map<String, Object> event = eventService.getSingleEvent(eventId);
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> event(@Valid @ModelAttribute PostEventDto postEventDto) {
        Map<String, Object> event = eventService.postSingleEvent(postEventDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    // Update And Delete Coming Soon

    @GetMapping("/search")
    ResponseEntity<Map<String, Object>> searchEvents(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size,
        @RequestParam(name = "searchTerm", required = true) String searchTerm) {
        Map<String, Object> mapEvent = eventService.searchEvents(page, size, searchTerm);
        return ResponseEntity.status(HttpStatus.OK).body(mapEvent);
    }
}
