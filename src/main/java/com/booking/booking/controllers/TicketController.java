package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.ticket.PostTicketDto;
import com.booking.booking.services.TicketService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

// @AllArgsConstructor
@RestController
@RequestMapping("/api")
public class TicketController {
    private final TicketService ticketService;

    @Autowired
    TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/protected/tickets")
    public ResponseEntity<Map<String, Object>> postNewTicket(@RequestBody PostTicketDto postTicketDto) {
        Map<String, Object> mapTicket = ticketService.postTicket(postTicketDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapTicket);
    }
    
    @GetMapping("/{ticketId}")
    public ResponseEntity<Map<String, Object>> getTicket(@PathVariable Long ticketId) {
        Map<String, Object> mapTicket = ticketService.getTicket(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(mapTicket);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTickets(
        @RequestParam(name = "page", defaultValue = "0", required = false) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        Map<String, Object> mapTicket = ticketService.getAllTickets(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(mapTicket);
    }
    
    @DeleteMapping("/{ticketId}")
    public String deleteTicket(@PathVariable Long ticketId) {
        Map<String, Object> mapTicket = ticketService.cancelTicket(ticketId);
        return "Wait for response";
    }
}
