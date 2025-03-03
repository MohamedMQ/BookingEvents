package com.booking.booking.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.booking.booking.utils.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(nullable = false, name = "ticket_id")
    private Ticket ticket;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, name = "transaction_id")
    private String transactionId;

    @Column(nullable = false)
    private PaymentStatus status;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}