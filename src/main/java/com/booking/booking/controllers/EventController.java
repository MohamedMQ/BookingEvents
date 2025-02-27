package com.booking.booking.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.event.PostEventDto;
import com.booking.booking.services.EventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    ResponseEntity<Map<String, Object>> events(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        Map<String, Object> mapEvent = eventService.getAllEvents(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(mapEvent);
    }

    @GetMapping("/:eventId")
    ResponseEntity<Map<String, Object>> event(@PathVariable("eventId") Integer eventId) {
        Map<String, Object> event = eventService.getSingleEvent(eventId);
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @PostMapping
    String event(@Valid @RequestBody PostEventDto postEventDto) {
        eventService.postSingleEvent(postEventDto);
        return "Sorry";
    }
}
