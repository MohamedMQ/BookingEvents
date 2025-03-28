package com.booking.booking.services;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.booking.booking.models.Event;
import com.booking.booking.models.Payment;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.PaymentRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.repositories.UserRepository;
import com.booking.booking.utils.PaymentStatus;
import com.booking.booking.utils.TicketStatus;
import com.stripe.model.checkout.Session;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Service
public class TicketProcessorService {
    @Value("${application.stripe.api_key}")
    private static String STRIPE_SECRET_KEY;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketQueueService ticketQueueService;
    private final PaymentRepository paymentRepository;

    // @Async
    // @Scheduled(fixedRate = 1000) // 300.000
    public void processPendingTickets() {
        System.out.println("Starting ticket processing...");
        List<Ticket> tickets = ticketRepository.findByStatus(TicketStatus.PENDING);
        if (tickets.isEmpty()) {
            System.out.println("No tickets to process.");
            return;
        }
        for (Ticket ticket : tickets) {
            if (ticket.getPayment().getStatus() == PaymentStatus.PENDING) {
                try {
                    Session session = Session.retrieve(ticket.getPayment().getSessionId());
                    Session expiredSession = session.expire();
                    if ("canceled".equals(expiredSession.getStatus())) {
                        System.out.println("✅ PaymentIntent " + session + " successfully canceled.");
                        ticket.setStatus(TicketStatus.CANCELED);
                        ticketRepository.save(ticket);
                        LinkedList<Long> eventQueue = ticketQueueService.getEventQueue(ticket.getEvent().getId());
                        if (!eventQueue.isEmpty()) {
                            Long queuedTicketId = eventQueue.removeFirst();
                            Ticket queuedTicket = ticketRepository.findById(queuedTicketId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + queuedTicketId));
                            queuedTicket.setStatus(TicketStatus.PENDING);
                            queuedTicket.setQueueNum(-1);
                            ticketRepository.save(queuedTicket);
                            Payment newPayment = Payment
                                .builder()
                                .ticket(queuedTicket)
                                .sessionId("")
                                .status(PaymentStatus.PENDING)
                                .amount(queuedTicket.getEvent().getPrice())
                                .build();
                            paymentRepository.save(newPayment);
                            // THIS SHOULD BE ASYNC TO NOT BLOCK THE RESPONSE 
                            eventQueue.forEach(tId -> {
                                // WILL BE ADDED LATER ON
                            });
                        }
                    } else {
                        System.out.println("⚠️ PaymentIntent " + session + " was not canceled. Status: " + expiredSession.getStatus());
                    }
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            System.out.println("ticket to process. " + ticket);
        }
    }
}
