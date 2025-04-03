package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.ticket.PostTicketDto;
import com.booking.booking.services.TicketService;

import lombok.AllArgsConstructor;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class TicketController {
    private final TicketService ticketService;

    /* NOT PROTECTED ROUTES */

    /* PROTECTED ROUTES */

    @GetMapping("/protected/tickets")
    public ResponseEntity<Map<String, Object>> protectedTickets(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        Map<String, Object> mapTicket = ticketService.getProtectedTickets(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(mapTicket);
    }

    @GetMapping("/protected/tickets/{ticketId}")
    public ResponseEntity<Map<String, Object>> protectedTicket(@PathVariable Long ticketId) {
        Map<String, Object> mapTicket = ticketService.getProtectedTicket(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(mapTicket);
    }

    @PostMapping("/protected/tickets")
    public ResponseEntity<Map<String, Object>> protectedTicket(@RequestBody PostTicketDto postTicketDto) {
        Map<String, Object> mapTicket = ticketService.postProtectedTicket(postTicketDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapTicket);
    }
    
    @DeleteMapping("/protected/tickets/{ticketId}")
    public ResponseEntity<Map<String, Object>> protectedTicketDelete(@PathVariable Long ticketId) {
        Map<String, Object> mapTicket = ticketService.cancelProtectedTicket(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(mapTicket);
    }
}
