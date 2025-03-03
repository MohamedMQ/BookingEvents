package com.booking.booking.services;

import org.springframework.stereotype.Service;

import com.booking.booking.repositories.PaymentRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
}