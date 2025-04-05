package com.booking.booking.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.booking.dto.ticket.PostTicketDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.Payment;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.PaymentRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.EventStatus;
import com.booking.booking.utils.PaymentStatus;
import com.booking.booking.utils.TicketStatus;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.awt.Color;
import java.awt.color.*;

@Getter
@Setter
@AllArgsConstructor
@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final TicketQueueService ticketQueueService;

    public Map<String, Object> getProtectedTickets(int page, int size) {
        Map<String, Object> mapTicket = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt"));
        Page<Event> events = ticketRepository.findAllByUserId(pageable, user.getId());
        // List<Event> eventList = events.stream().toList();

        List<Event> eventList = events.stream().map(event -> {
            Set<Ticket> filteredTickets = event.getTickets().stream()
                .filter(ticket -> {
                    System.err.println(ticket.getStatus());
                    return ((ticket.getStatus() == TicketStatus.CONFIRMED
                        || ticket.getStatus() == TicketStatus.DESTROYED)
                        && ticket.getUser().getId() == user.getId());
                })
                .collect(Collectors.toSet());
            event.setTickets(filteredTickets);
            return event;
        }).toList();

        mapPagination.put("currentPage", events.getNumber());
        mapPagination.put("pageSize", events.getSize());
        mapPagination.put("totalElements", events.getTotalElements());
        mapPagination.put("totalPages", events.getTotalPages());
        mapPagination.put("firstPage", events.isFirst());
        mapPagination.put("lastPage", events.isLast());
        mapTicket.put("status", "success");
        mapTicket.put("message", "Tickets retrieved successfully.");
        mapTicket.put("data", eventList);
        mapTicket.put("pagination", mapPagination);
        return mapTicket;
    }

    public Map<String, Object> getProtectedTicket(Long ticketId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, user.getId()).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + ticketId + " " + user.getId()));
        Event event = eventRepository.findById(ticket.getEvent().getId()).orElseThrow(() -> new EntityNotFoundException("No event found with this ID " + ticket.getEvent().getId()));
        
        System.out.println(ticketId + " | " + ticket.getId());

        Set<Ticket> filteredTickets = event.getTickets().stream()
            .filter(t -> (t.getUser().getId() == user.getId()
                && (t.getStatus() == TicketStatus.CONFIRMED
                || t.getStatus() == TicketStatus.DESTROYED)))
            .collect(Collectors.toSet());    
        event.setTickets(filteredTickets);

        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket retrieved successfully.");
        mapTicket.put("data", event);
        return mapTicket;
    }

    @Transactional
    public Map<String, Object> postProtectedTicket(PostTicketDto postTicketDto) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<EventStatus> eventStatusList = new ArrayList<>(Arrays.asList(EventStatus.DESTROYED, EventStatus.DESTROYING));
        Event event = eventRepository.findByIdAndEventStatusNotIn(postTicketDto.getEventId(), eventStatusList).orElseThrow(() -> new EntityNotFoundException("No valid event found with the given ID " + postTicketDto.getEventId()));
        if (user.getId() == event.getUser().getId())
            throw new EntityNotFoundException("You cannot book a ticket for your own event");
        if (LocalDateTime.now().isAfter(event.getEventDateTime().minusMinutes(35)))
            throw new EntityNotFoundException("The event close to start, already started or finished");
        // long countTicketsConfirmed = ticketRepository.countByStatus(TicketStatus.CONFIRMED);
        if (event.getAvailableTickets() == 0)
            throw new EntityNotFoundException("all the tickets has been sold");
        Optional<Ticket> ticket = ticketRepository.findByUserIdAndEventIdAndStatusNotIn(
            user.getId(), 
            event.getId(), 
            List.of(TicketStatus.CANCELED, TicketStatus.QUEUED)
        );
        if (ticket.isPresent())
            throw new EntityNotFoundException("You already purchased or booked a ticket for this event");
        System.err.println(event.getTicketNumber());
        if (event.getTicketNumber() == 0) {
            System.err.println("FDKSHAFKJAHFKJDFKLJAF=DSF==DS=F-DS=F=D-F=-D=F-ASDF-=DS");
            Optional<Ticket> queuedTicket = ticketRepository.findByUserIdAndEventIdAndStatus(user.getId(), event.getId(), TicketStatus.QUEUED);
            if (queuedTicket.isPresent())
                throw new EntityNotFoundException("You'are already been added to the queue");
            Ticket ticketToSend = ticketQueueService.addToEventQueue(user, event);
            Map<String, Object> mapTicket = new HashMap<>();
            mapTicket.put("status", "success");
            mapTicket.put("message", "You've been added to the queue successfully");
            mapTicket.put("data", ticketToSend);
            return mapTicket;
        }
        event.setTicketNumber(event.getTicketNumber() - 1);
        eventRepository.save(event);
        Ticket newTicket = Ticket
            .builder()
            .user(user)
            .event(event)
            .status(TicketStatus.PENDING)
            .sessionId(null)
            .queueNum(-1)
            .qrCodeUrl("")
            .build();
        newTicket = ticketRepository.save(newTicket);
        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket added successfully.");
        mapTicket.put("data", newTicket);
        return mapTicket;
    }

    @Transactional
    public Map<String, Object> cancelProtectedTicket(Long tickedId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findById(tickedId).orElseThrow(() -> new EntityNotFoundException("No ticket().getEventDateTime().isEqual(LocalDateTime.now()) || ticket.getEvent().getEventDateTime().isBefore(LocalDateTime.now())t found with this ID " + tickedId));
        Event event = eventRepository.findById(ticket.getEvent().getId()).orElseThrow(() -> new EntityNotFoundException("No event found with this ID " + ticket.getEvent().getId()));
        if (ticket.getUser().getId() != user.getId())
        throw new EntityNotFoundException("You are not autorized to cancel other's tickets");
        if (ticket.getStatus() != TicketStatus.PENDING)
            throw new EntityNotFoundException("Your ticket already canceled, confirmed or destroyed");
        if (LocalDateTime.now().isAfter(event.getEventDateTime().minusMinutes(30)))
            throw new EntityNotFoundException("The event close to start, already started or finished");
        ticket.setStatus(TicketStatus.CANCELED);
        ticketRepository.save(ticket);
        event.setTicketNumber(event.getTicketNumber() + 1);
        eventRepository.save(event);
        System.err.println(event.getTicketNumber());

        LinkedList<Long> queueTicketList = ticketQueueService.getEventQueue(ticket.getEvent().getId());
        if (!queueTicketList.isEmpty()) {
            Long queuedTicketId = queueTicketList.removeFirst();
            Ticket queuedTicket = ticketRepository.findById(queuedTicketId).orElseThrow(() -> new EntityNotFoundException("No ticket found with this ID " + queuedTicketId));
            queuedTicket.setStatus(TicketStatus.PENDING);
            queuedTicket.setQueueNum(-1);
            ticketRepository.save(queuedTicket);
            
            // THIS SHOULD BE ASYNC TO NOT BLOCK THE RESPONSE 
            queueTicketList.forEach(tId -> {
                // WILL BE ADDED LATER ON
            });
        }

        System.err.println(ticket);
        Map<String, Object> mapTicket = new HashMap<>();
        mapTicket.put("status", "success");
        mapTicket.put("message", "Ticket canceled successfully.");
        mapTicket.put("data", ticket);
        return mapTicket;
    }

    public ResponseEntity<byte[]> getPdfFile(Long ticketId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new EntityNotFoundException("No ticket().getEventDateTime().isEqual(LocalDateTime.now()) || ticket.getEvent().getEventDateTime().isBefore(LocalDateTime.now())t found with this ID " + ticketId));
        Event event = eventRepository.findById(ticket.getEvent().getId()).orElseThrow(() -> new EntityNotFoundException("No event found with this ID " + ticket.getEvent().getId()));
        if (ticket.getUser().getId() != user.getId())
            throw new EntityNotFoundException("You are not autorized to cancel other's tickets");
        if (ticket.getStatus() != TicketStatus.CONFIRMED)
            throw new EntityNotFoundException("Your ticket is not confirmed yet");
        if (LocalDateTime.now().isAfter(event.getEventDateTime().plusMinutes(15)))
            throw new EntityNotFoundException("The event finished or close to finish");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph());
    
            Image image = Image.getInstance('.' + event.getImageUrl());
            float documentWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
            image.scaleToFit(documentWidth, document.getPageSize().getHeight());
            image.setAlignment(Image.ALIGN_CENTER);
            document.add(image);

            document.add(new Paragraph());

            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            Font headerFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
            PdfPCell headerCell = new PdfPCell(new Phrase("Tickey Summary", headerFont));
            headerCell.setBackgroundColor(Color.RED);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(12f);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(headerCell);
            headerTable.setSpacingBefore(20f);
            document.add(headerTable);

            Image qrImage = Image.getInstance('.' + ticket.getQrCodeUrl());
            qrImage.setAlignment(Image.ALIGN_CENTER);
            qrImage.scaleToFit(150, 150);
            document.add(qrImage);

            Paragraph eventName = new Paragraph("Event: " + event.getName());
            eventName.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(eventName);
            Paragraph eventDate = new Paragraph("Date: " + event.getEventDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss")));
            eventDate.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(eventDate);
            Paragraph eventLocation = new Paragraph("Location: " + event.getLocation());
            eventLocation.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(eventLocation);
            Paragraph ticketHolder = new Paragraph("Ticket Holder: " + ticket.getUser().getName());
            ticketHolder.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(ticketHolder);
            Paragraph eventPrice = new Paragraph("Price: $" + event.getPrice());
            eventPrice.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(eventPrice);
            Paragraph ticketPurchasedDate = new Paragraph("Purchased Date: " + ticket.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss")));
            ticketPurchasedDate.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(ticketPurchasedDate);
            document.add(new Paragraph());

            PdfPTable headerTable2 = new PdfPTable(1);
            headerTable2.setWidthPercentage(100);
            Font headerFont2 = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
            PdfPCell headerCell2 = new PdfPCell(new Phrase("Important Informations", headerFont2));
            headerCell2.setBackgroundColor(Color.RED);
            headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell2.setPadding(12f);
            headerCell2.setBorder(Rectangle.NO_BORDER);
            headerTable2.addCell(headerCell2);
            headerTable2.setSpacingBefore(20f);
            document.add(headerTable2);

            Paragraph note1 = new Paragraph("• Please arrive at least 30 minutes before the event");
            Paragraph note2 = new Paragraph("• Have your ticket QR code ready for scanning");
            Paragraph note3 = new Paragraph("• This ticket is non-transferable");
            note1.setSpacingBefore(20f);
            document.add(note1);
            document.add(note2);
            document.add(note3);

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "event_ticket.pdf");
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new EntityNotFoundException("Something went wrong!! try again later");
        }
    }
}