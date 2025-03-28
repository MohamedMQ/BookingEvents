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
@RequestMapping("/api")
public class EventController {
    private final EventService eventService;

    /* NOT PROTECTED ROUTES */

    @GetMapping("/public/events")
    ResponseEntity<Map<String, Object>> publicEvents(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        System.out.println("INSIDE EVENTS PUBLIC");
        Map<String, Object> mapEvent = eventService.getPublicEvents(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(mapEvent);
    }

    @GetMapping("/public/events/{eventId}")
    ResponseEntity<Map<String, Object>> publicEvent(@PathVariable("eventId") Long eventId) {
        Map<String, Object> event = eventService.getPublicEvent(eventId);
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @GetMapping("/public/events/search")
    ResponseEntity<Map<String, Object>> publicEventSearch(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size,
        @RequestParam(name = "searchTerm", required = true) String searchTerm) {
        Map<String, Object> mapEvent = eventService.getPublicEventSearch(page, size, searchTerm);
        return ResponseEntity.status(HttpStatus.OK).body(mapEvent);
    }

    /* PROTECTED ROUTES */
    
    @GetMapping("/protected/events")
    ResponseEntity<Map<String, Object>> protectedEvents(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        Map<String, Object> mapEvent = eventService.getProtectedEvents(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(mapEvent);
    }

    @GetMapping("/protected/events/me")
    public ResponseEntity<Map<String, Object>> protectedEventsOwn(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        Map<String, Object> mapEvent = eventService.getProtectedEventsOwn(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(mapEvent);
    }
    

    @GetMapping("/protected/events/{eventId}")
    ResponseEntity<Map<String, Object>> protectedEvent(@PathVariable("eventId") Long eventId) {
        Map<String, Object> event = eventService.getProtectedEvent(eventId);
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }
    
    @PostMapping("/protected/events")
    ResponseEntity<Map<String, Object>> protectedEvent(@Valid @ModelAttribute PostEventDto postEventDto) {
        Map<String, Object> event = eventService.postProtectedSingleEvent(postEventDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    // Update and Delete event Coming Soon
}
    