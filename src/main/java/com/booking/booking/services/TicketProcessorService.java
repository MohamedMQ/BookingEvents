package com.booking.booking.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.booking.booking.models.Ticket;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.PaymentStatus;
import com.booking.booking.utils.TicketStatus;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

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
                    } else {
                        System.out.println("⚠️ PaymentIntent " + session + " was NOT canceled. Status: " + expiredSession.getStatus());
                    }
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            System.out.println("ticket to process. " + ticket);
        }
    }
}
