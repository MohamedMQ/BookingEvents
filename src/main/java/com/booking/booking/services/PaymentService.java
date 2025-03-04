package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.payment.PostPaymentDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.Payment;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.PaymentRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.PaymentStatus;
import com.booking.booking.utils.StatusEnum;
import com.booking.booking.utils.TicketStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Service
public class PaymentService {
    @Value("${application.stripe.api_key}")
    private String apiKey;

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    @Autowired
    PaymentService(PaymentRepository paymentRepository, TicketRepository ticketRepository, EventRepository eventRepository) {
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    public Map<String, Object> chargeCreditCard(PostPaymentDto postPaymentDto) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findById(postPaymentDto.getTicketId()).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + postPaymentDto.getTicketId()));
        Event event = eventRepository.findById(ticket.getEvent().getId()).orElseThrow(() -> new EntityNotFoundException("No event found with this ID " + postPaymentDto.getTicketId()));
        if (user.getId() != ticket.getUser().getId())
            throw new EntityNotFoundException("Your are not authorized to access or pay for others tickets");
        if (user.getId() == ticket.getEvent().getUser().getId())
            throw new EntityNotFoundException("You cannot pay for your own event");
        if (ticket.getStatus() != TicketStatus.PENDING)
            throw new EntityNotFoundException("This ticket already confirmed or canceled");
        if (ticket.getEvent().getEventDateTime().isAfter(LocalDateTime.now()))
            throw new EntityNotFoundException("This ticket exprired or already used");
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long)(ticket.getEvent().getPrice() * 100))
                .setCurrency("usd")
                .setPaymentMethod(postPaymentDto.getStripeToken())
                .setDescription("${user.getId()} paying his ticket")
                .setConfirm(true)
                .build();
            PaymentIntent intent = PaymentIntent.create(params);
            ticket.setStatus(TicketStatus.CONFIRMED);
            ticketRepository.save(ticket);
            Payment payment = Payment.builder()
                .ticket(ticket)
                .amount(ticket.getEvent().getPrice())
                .transactionId(intent.getId())
                .status(PaymentStatus.SUCCESS)
                .build();
            payment = paymentRepository.save(payment);
            Map<String, Object> mapPayment = new HashMap<>();
            mapPayment.put("status", "success");
            mapPayment.put("message", "payment history created successfully");
            mapPayment.put("data", payment);
            return mapPayment;
        } catch (StripeException e) {
            ticket.setStatus(TicketStatus.CANCELED);
            ticketRepository.save(ticket);
            event.setAvailableTickets(event.getAvailableTickets() + 1);
            eventRepository.save(event);
            Payment payment = Payment.builder()
                .ticket(ticket)
                .amount(ticket.getEvent().getPrice())
                .transactionId(postPaymentDto.getStripeToken())
                .status(PaymentStatus.FAILED)
                .build();
            paymentRepository.save(payment);
            throw new EntityNotFoundException("Payment failed :( try reserving and paying again");
        }
    }
}