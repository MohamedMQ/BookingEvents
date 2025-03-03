package com.booking.booking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booking.booking.models.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
}