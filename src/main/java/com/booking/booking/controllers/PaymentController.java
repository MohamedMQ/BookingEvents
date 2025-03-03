package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.payment.PostPaymentDto;
import com.booking.booking.services.PaymentService;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@AllArgsConstructor
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public String postMethodName(@RequestBody PostPaymentDto postPaymentDto) {
        return entity;
    }
}