package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.services.PaymentService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/protected/payments/{ticketId}")
    public ResponseEntity<Map<String, Object>> initializePayment(@PathVariable Long ticketId) {
        Map<String, Object> mapPayment = paymentService.generateStripeSession(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(mapPayment);
    }

    @GetMapping("/protected/payments/stripe/{ticketId}")
    public String handleReturn(
        @PathVariable("ticketId") Long ticketId,
        @RequestParam(name = "session_id", required = true) String sessionId,
        HttpServletResponse response) {
        paymentService.paymentStatus(ticketId, sessionId, response);
        return new String();
    }
}