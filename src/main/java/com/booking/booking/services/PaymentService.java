package com.booking.booking.services;

import static java.io.File.separator;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.booking.booking.models.Event;
import com.booking.booking.models.Payment;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.PaymentRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.PaymentStatus;
import com.booking.booking.utils.TicketStatus;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.stripe.model.Account;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter; 

import lombok.Setter;

@Getter
@Setter
@Service
public class PaymentService {
    @Value("${spring.web.resources.static-locations}")
    private String fileUploadPath;

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public PaymentService(PaymentRepository paymentRepository, TicketRepository ticketRepository,
            EventRepository eventRepository) {
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    public Map<String, Object> generateStripeSession(Long ticketId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + ticketId));
        if (user.getId() != ticket.getUser().getId())
            throw new EntityNotFoundException("Your are not authorized to access or pay for others tickets");
        if (ticket.getStatus() != TicketStatus.PENDING)
            throw new EntityNotFoundException("This ticket already confirmed, canceled or still in the queue");
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(ticket.getEvent().getEventDateTime().minusMinutes(60)))
            throw new EntityNotFoundException("This ticket exprired");
        ticket.setSessionId(null);
        ticketRepository.save(ticket);
        Map<String, String> mapSessionId = new HashMap<>();
        String YOUR_DOMAIN = "http://localhost:3000";
        try {
            String successUrl = "http://localhost:8080/api/protected/payments/stripe/" + ticket.getId() + "?session_id={CHECKOUT_SESSION_ID}";
            Account account = Account.retrieve(user.getAccountId());
            System.out.println("Seller Account ID: " + user.getAccountId());
            System.out.println("Seller Capabilities: " + account.getCapabilities());
            System.out.println("Seller Payouts Enabled: " + account.getPayoutsEnabled());

            SessionCreateParams params = SessionCreateParams
                .builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(YOUR_DOMAIN)
                .addLineItem(
                    SessionCreateParams.LineItem
                        .builder()
                        .setQuantity(1L)
                        .setPrice(ticket.getEvent().getStripePrice())
                        .build()
                )
                .setPaymentIntentData(
                    SessionCreateParams.PaymentIntentData.builder()
                        .setTransferData(
                            SessionCreateParams.PaymentIntentData.TransferData.builder()
                                .setDestination(ticket.getEvent().getUser().getAccountId())
                                .build()   
                        )
                        .build()
                )
                .build();
            System.out.println("BEFORE CREATING THE SESSION");
            Session session = Session.create(params);
            System.out.println("AFTER CREATING THE SESSION");
            ticket.setSessionId(session.getId());
            ticketRepository.save(ticket);
            System.out.println("USERNAME - " + ticket.getUser().getName() + " - EVENTNAME - " + ticket.getEvent().getName() + " - TICKETID - " + ticket.getId());
            mapSessionId.put("sessionId", session.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw new EntityNotFoundException("Something went wrong!! try again");
        }
        Map<String, Object> mapSession = new HashMap<>();
        mapSession.put("status", "success");
        mapSession.put("message", "Session created successfully.");
        mapSession.put("data", mapSessionId);
        return mapSession;
    }

    private String generateQRCode(Ticket ticket) {
        String qrCodeData = "https://localhost:8080/api/protected/tickets/" + ticket.getId();
        int width = 300, height = 300;
        String qrCodeFilePath = null;
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(qrCodeData, BarcodeFormat.QR_CODE, width, height);
            final String subPath = "tickets" + separator + ticket.getUser().getId() + separator + ticket.getEvent().getId();
            final String fullPath = fileUploadPath + separator + subPath;
            File targetFolder = new File(fullPath);
            if (!targetFolder.exists())
                if (!targetFolder.mkdirs())
                    return null;
            qrCodeFilePath = fullPath + separator + "qrcode.png";
            Path path = FileSystems.getDefault().getPath(qrCodeFilePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        } catch (Exception e) {
            return null;
        }
        return qrCodeFilePath.substring(1);
    }

    @Transactional
    public void paymentStatus(Long ticketId, String sessionId, HttpServletResponse response) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + ticketId));
            Session session = Session.retrieve(ticket.getSessionId());
            if ("complete".equals(session.getStatus())) {
                Event event = eventRepository.findById(ticket.getEvent().getId()).orElseThrow(() -> new EntityNotFoundException("No event found with this ID " + ticket.getEvent().getId()));
                event.setAvailableTickets(event.getAvailableTickets() - 1);
                eventRepository.save(event);
                ticket.setStatus(TicketStatus.CONFIRMED);
                ticketRepository.save(ticket);
                Payment payment = Payment.builder()
                    .ticket(ticket)
                    .amount(event.getPrice())
                    .status(PaymentStatus.SUCCESS)
                    .build();
                paymentRepository.save(payment);
                String qrCodeFilePath = generateQRCode(ticket);
                if(qrCodeFilePath != null) {
                    ticket.setQrCodeUrl(qrCodeFilePath);
                    ticketRepository.save(ticket);
                }


                Session session1 = Session.retrieve(ticket.getSessionId());
                System.out.println("Session1 PaymentIntent: " + session1.getPaymentIntent());
                System.out.println("Session PaymentIntent: " + session.getPaymentIntentObject());
                PaymentIntent paymentIntent = PaymentIntent.retrieve(
                    session.getPaymentIntent()
                );
                String chargeId = paymentIntent.getLatestCharge();
                System.out.println("Charge ID: " + chargeId);





                response.sendRedirect("http://localhost:3000/events/" + ticket.getEvent().getId());
            } else {
                ticket.setSessionId(null);
                ticketRepository.save(ticket);
                response.sendRedirect("http://localhost:3000/events");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new EntityNotFoundException("Something went when returned from stripe checkout");
        }
    }
}