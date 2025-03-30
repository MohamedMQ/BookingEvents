package com.booking.booking.services;

import static java.io.File.separator;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
// @AllArgsConstructor
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
        Map<String, String> mapSessionId = new HashMap<>();
        String YOUR_DOMAIN = "http://localhost:3000";
        try {
            String successUrl = "http://localhost:8080/api/protected/payments/stripe/" + ticket.getId() + "?session_id={CHECKOUT_SESSION_ID}";
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
                .build();
            Session session = Session.create(params);
            mapSessionId.put("sessionId", session.getId());
        } catch (Exception e) {
            System.err.println(e);
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
            Session session = Session.retrieve(sessionId);
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + ticketId));
            if ("complete".equals(session.getStatus())) {
                Event event = eventRepository.findById(ticket.getEvent().getId()).orElseThrow(() -> new EntityNotFoundException("No event found with this ID " + ticket.getEvent().getId()));
                Payment payment = paymentRepository.findById(ticket.getPayment().getId()).orElseThrow(() -> new EntityNotFoundException("No payment found with this ID " + ticket.getPayment().getId()));
                event.setAvailableTickets(event.getAvailableTickets() - 1);
                eventRepository.save(event);
                ticket.setStatus(TicketStatus.CONFIRMED);
                ticketRepository.save(ticket);
                payment.setStatus(PaymentStatus.SUCCESS);
                paymentRepository.save(payment);
                String qrCodeFilePath = generateQRCode(ticket);
                if(qrCodeFilePath != null) {
                    ticket.setQrCodeUrl(qrCodeFilePath);
                    ticketRepository.save(ticket);
                }
                System.out.println("Payment Successed");
                response.sendRedirect("http://localhost:3000/events/" + ticket.getEvent().getId());
            } else {
                System.out.println("Payment failed");
                response.sendRedirect("http://localhost:3000/events");
            }
        } catch (Exception e) {
            throw new EntityNotFoundException("Something went when returned from stripe checkout");
        }
    }
}