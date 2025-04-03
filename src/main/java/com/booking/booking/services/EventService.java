package com.booking.booking.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.booking.booking.dto.event.PostEventDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.Payment;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.PaymentRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.EventStatus;
import com.booking.booking.utils.TicketStatus;
import com.stripe.model.Account;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundCreateParams.Reason;

import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.Setter;

import static java.io.File.separator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

@Getter
@Setter
// @AllArgsConstructor
@Service
public class EventService {
    @Value("${spring.web.resources.static-locations}")
    private String fileUploadPath;

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;

    EventService(
        EventRepository eventRepository,
        TicketRepository ticketRepository,
        PaymentRepository paymentRepository
    ) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
    }

    // NOT PROTECTED SERVICES

    public Map<String, Object> getPublicEvents(
        int page,
        int size) {
        Map<String, Object> mapEvent = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<EventStatus> eventStatusList = new ArrayList<>(Arrays.asList(EventStatus.DESTROYED, EventStatus.DESTROYING));
        Page<Event> events = eventRepository.findByEventStatusNotIn(pageable, eventStatusList);
        // List<Event> eventList = events.stream().toList();

        List<Event> eventList = events.stream().map(event -> {
            Set<Ticket> emptyTickets = Set.of();
            event.setTickets(emptyTickets);
            return event;
        }).toList();

        mapPagination.put("currentPage", events.getNumber());
        mapPagination.put("pageSize", events.getSize());
        mapPagination.put("totalElements", events.getTotalElements());
        mapPagination.put("totalPages", events.getTotalPages());
        mapPagination.put("firstPage", events.isFirst());
        mapPagination.put("lastPage", events.isLast());
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", eventList);
        mapEvent.put("pagination", mapPagination);
        return mapEvent;
    }
    
    public Map<String, Object> getPublicEvent(Long eventId) {
        Map<String, Object> mapEvent = new HashMap<>();
        List<EventStatus> eventStatusList = new ArrayList<>(Arrays.asList(EventStatus.DESTROYED, EventStatus.DESTROYING));
        Event event = eventRepository
                .findByIdAndEventStatusNotIn(eventId, eventStatusList)
                .orElseThrow(() -> new EntityNotFoundException("No event found with ID " + eventId));
        
        Set<Ticket> emptyTickets = Set.of();
        event.setTickets(emptyTickets);
        
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", event);
        return mapEvent;
    }

    public Map<String, Object> getPublicEventSearch(
        int page,
        int size,
        String searchTerm) {
        Map<String, Object> mapEvent = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> events = eventRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrLocationContainingIgnoreCase(pageable, searchTerm, searchTerm, searchTerm);
        List<Event> eventList = events.stream().toList();
        mapPagination.put("currentPage", events.getNumber());
        mapPagination.put("pageSize", events.getSize());
        mapPagination.put("totalElements", events.getTotalElements());
        mapPagination.put("totalPages", events.getTotalPages());
        mapPagination.put("firstPage", events.isFirst());
        mapPagination.put("lastPage", events.isLast());
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", eventList);
        mapEvent.put("pagination", mapPagination);
        return mapEvent;
    }

    // PROTECTED SERVICES

    public Map<String, Object> getProtectedEvents(
        int page,
        int size) {
        Map<String, Object> mapEvent = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Page<Event> events = eventRepository.findAllEventsWithUserTickets(pageable, user.getId());
        // List<Event> eventList = events.stream().toList();
        
        List<EventStatus> eventStatusList = new ArrayList<>(Arrays.asList(EventStatus.DESTROYED, EventStatus.DESTROYING));
        Page<Event> events = eventRepository.findByEventStatusNotIn(pageable, eventStatusList);
        List<Event> eventList = events.stream().map(event -> {
            Set<Ticket> filteredTickets = event.getTickets().stream()
                .filter(ticket -> {
                    // System.err.println(ticket.getStatus());
                    return (ticket.getStatus() != TicketStatus.CANCELED
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
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", eventList);
        mapEvent.put("pagination", mapPagination);
        return mapEvent;
    }

    public Map<String, Object> getProtectedEventsOwn(int page, int size) {
        Map<String, Object> mapEvent = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());        
        Page<Event> events = eventRepository.findByUserId(pageable, user.getId());
        // List<Event> eventList = events.stream().toList();

        List<Event> eventList = events.stream().map(event -> {
            Set<Ticket> filteredTickets = event.getTickets().stream()
                .filter(ticket -> 
                    ticket.getStatus() == TicketStatus.CONFIRMED
                    || ticket.getStatus() == TicketStatus.DESTROYED)
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
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", eventList);
        mapEvent.put("pagination", mapPagination);
        return mapEvent;
    }

    public Map<String, Object> getProtectedEvent(Long eventId) {
        Map<String, Object> mapEvent = new HashMap<>();
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<EventStatus> eventStatusList = new ArrayList<>(Arrays.asList(EventStatus.DESTROYED, EventStatus.DESTROYING));
        Event event = eventRepository
            .findByIdAndEventStatusNotIn(eventId, eventStatusList)
            .orElseThrow(() -> new EntityNotFoundException("No event found with eventId " + eventId));    

        Set<Ticket> filteredTickets = event.getTickets().stream()
            .filter((ticket -> (ticket.getStatus() != TicketStatus.CANCELED)
                && ticket.getUser().getId() == user.getId()))
            .collect(Collectors.toSet());    
        event.setTickets(filteredTickets);
        
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", event);
        return mapEvent;
    }    

    public Map<String, Object> postProtectedSingleEvent(PostEventDto postEventDto) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LocalDateTime now = LocalDateTime.now();
        if (!postEventDto.getEventDateTime().isAfter(now.plusDays(1)))
            throw new EntityNotFoundException("eventDateTime value is invalid " + postEventDto.getEventDateTime() + ". it must be after 1 day from now.");
        if (postEventDto.getImage() != null && !postEventDto.getImage().isEmpty() && !postEventDto.getImage().getContentType().startsWith("image/"))
            throw new EntityNotFoundException("The file provided is not an image");
        Event event;
        try {
            Account account = Account.retrieve(user.getAccountId());
            List<String> currentlyDue = account.getRequirements().getCurrentlyDue();
            String disabledReason = account.getRequirements().getDisabledReason();
            String acceptPayments = account.getCapabilities().getTransfers();
            if (!currentlyDue.isEmpty()
                || disabledReason != null
                || acceptPayments == null)
                throw new Error();

            ProductCreateParams productCreateParams = ProductCreateParams.builder()
                .setName(postEventDto.getName())
                .setDescription(postEventDto.getDescription())
                .build();
            Product product = Product.create(productCreateParams);
            String productId = product.getId();
            Long priceInCents = Math.round(postEventDto.getPrice() * 100);
            PriceCreateParams priceCreateParams = PriceCreateParams.builder()
                    .setCurrency("usd")
                    .setUnitAmount(priceInCents)
                    .setProduct(productId)
                    .build();
            Price priceStripe = Price.create(priceCreateParams);
            String imagePath = (postEventDto.getImage() != null && !postEventDto.getImage().isEmpty()) ? saveImage(postEventDto.getImage(), user, null) : getDefaultImagePath(null);
            event = Event
                .builder()
                .name(postEventDto.getName())
                .description(postEventDto.getDescription())
                .location(postEventDto.getLocation())
                .category(postEventDto.getCategory())
                .eventDateTime(postEventDto.getEventDateTime())
                .price(postEventDto.getPrice())
                .totalTickets(postEventDto.getTotalTickets())
                .availableTickets(postEventDto.getTotalTickets())
                .TicketNumber(postEventDto.getTotalTickets())
                .eventStatus(EventStatus.CONFIRMED)
                .user(user)
                .imageUrl(imagePath)
                .stripePrice(priceStripe.getId())
                .build();
            event = eventRepository.save(event);
        } catch (Exception e) {
            throw new EntityNotFoundException("Something Went Wrong. try again later");
        }
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("status", "success");
        mapEvent.put("message", "Event created successfully.");
        mapEvent.put("data", event);
        return mapEvent;
    }
        
    private String saveImage(MultipartFile image, User user, Event event) {
        final String subPath = "users" + separator + "events" + separator + user.getId();
        final String fullPath = fileUploadPath + separator + subPath;
        File targetFolder = new File(fullPath);
        if (!targetFolder.exists())
            if (!targetFolder.mkdirs())
                return null;
        String fileName = image.getOriginalFilename();
        String targetFilePath = targetFolder + separator + System.currentTimeMillis() + ".";
        if (fileName != null && !fileName.isEmpty() && fileName.lastIndexOf(".") != -1)
            targetFilePath += fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        try (InputStream inputStream = new BufferedInputStream(image.getInputStream());
            OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Path.of(targetFilePath)))) {
            byte buffer[] = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1)
                outputStream.write(buffer, 0, bytesRead);
            if (event != null)
                removeOldImage(event.getImageUrl());
            return targetFilePath.substring(1);
        } catch (Exception e) {
            System.out.println("Failed to write the image to the destination " + targetFilePath);
        }
        return null;
    }

    private String getDefaultImagePath(Event event) {
        final String subPath = "users" + separator + "events";
        final String filePath = fileUploadPath + separator + subPath + separator + "default.jpg";
        if (event != null)
            removeOldImage(event.getImageUrl());
        return filePath.substring(1);
    }

    private void removeOldImage(String oldImagePath) {
        if (oldImagePath.contains("default.jpg"))
            return;
        String fullOldImagePath = '.' + oldImagePath;
        File imageFile = new File(fullOldImagePath);
        if (imageFile.exists()) {
            if (imageFile.delete())
                System.out.println("Image with path " + fullOldImagePath + " deleted successfully");
            else
                System.out.println("Image with path " + fullOldImagePath + " deleted failed");
        } else
            System.out.println("Image with path " + fullOldImagePath + " not found");
    }

    // @Transactional
    public Map<String, Object> deleteProtectedEvent(Long eventId) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Event event = eventRepository.findByIdAndUserIdAndEventStatus(eventId, user.getId(), EventStatus.CONFIRMED).orElseThrow(() -> new EntityNotFoundException("No event found in your events with this Id " + eventId));
        Event event = eventRepository.findByIdAndUserId(eventId, user.getId()).orElseThrow(() -> new EntityNotFoundException("No event found in your events with this Id " + eventId));
        if (event.getEventDateTime().isAfter(LocalDateTime.now())) {
            List<Ticket> confirmedTicketList = ticketRepository.findByEventIdAndStatusIn(eventId, new ArrayList<>(Arrays.asList(TicketStatus.CONFIRMED, TicketStatus.PENDING)));
            if (!confirmedTicketList.isEmpty())
                throw new EntityNotFoundException("Cannot cancel your event, already bought");
        }
        event.setEventStatus(EventStatus.DESTROYED);
        eventRepository.saveAndFlush(event);
        Event eventToSend = eventRepository.findById(eventId).orElseThrow(() -> new EntityNotFoundException("No event found in your events with this Id " + eventId));
        Set<Ticket> filteredTickets = eventToSend.getTickets().stream()
            .filter((ticket -> ticket.getStatus() == TicketStatus.CONFIRMED
                || ticket.getStatus() == TicketStatus.DESTROYED))
            .collect(Collectors.toSet());
        eventToSend.setTickets(filteredTickets);
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("status", "success");
        mapEvent.put("message", "Event updated successfully.");
        mapEvent.put("data", eventToSend);
        return mapEvent;
    }

    @Transactional
    public Map<String, Object> putProtectedSingleEvent(Long eventId, PostEventDto postEventDto) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // List<EventStatus> eventStatusList = new ArrayList<>(Arrays.asList(EventStatus.DESTROYED, EventStatus.DESTROYING));
        List<EventStatus> eventStatusList = new ArrayList<>(Arrays.asList(EventStatus.DESTROYED, EventStatus.DESTROYING));
        Event event = eventRepository.findByIdAndEventStatusNotIn(eventId, eventStatusList).orElseThrow(() -> new EntityNotFoundException("No valid event found with this Id " + eventId));
        if (event.getUser().getId() != user.getId())
            throw new EntityNotFoundException("You are not authorized to modify others events");
        List<Ticket> tickets = ticketRepository.findByEventIdAndStatusNot(eventId, TicketStatus.CANCELED);
        if (!tickets.isEmpty()) {
            if (!postEventDto.getEventDateTime().equals(event.getEventDateTime()))
                throw new EntityNotFoundException("EventDateTime value should not be changed " + postEventDto.getEventDateTime());
            if (!postEventDto.getLocation().equals(event.getLocation()))
                throw new EntityNotFoundException("Location value should not be changed " + postEventDto.getLocation());
            if (!postEventDto.getPrice().equals(event.getPrice()))
                throw new EntityNotFoundException("Price value should not be changed " + postEventDto.getPrice());
            if (postEventDto.getTotalTickets() < event.getTotalTickets())
                throw new EntityNotFoundException("TotalTickets value should only be increased or the same " + postEventDto.getTotalTickets());
        } else {
            LocalDateTime now = LocalDateTime.now();
            if (!postEventDto.getEventDateTime().isAfter(now.plusDays(1)))
                throw new EntityNotFoundException("eventDateTime value is invalid" + postEventDto.getEventDateTime());
        }
        if (postEventDto.getImage() != null && !postEventDto.getImage().isEmpty() && !postEventDto.getImage().getContentType().startsWith("image/"))
            throw new EntityNotFoundException("The file provided is not an image");
        String imagePath = (postEventDto.getImage() != null && !postEventDto.getImage().isEmpty()) ? saveImage(postEventDto.getImage(), user, event) : getDefaultImagePath(event);
        event.setName(postEventDto.getName());
        event.setDescription(postEventDto.getDescription());
        event.setLocation(postEventDto.getLocation());
        event.setCategory(postEventDto.getCategory());
        event.setEventDateTime(postEventDto.getEventDateTime());
        event.setPrice(postEventDto.getPrice());
        event.setAvailableTickets(event.getAvailableTickets() + (postEventDto.getTotalTickets() - event.getTotalTickets()));
        event.setTicketNumber(event.getTicketNumber() + (postEventDto.getTotalTickets() - event.getTotalTickets()));
        event.setTotalTickets(postEventDto.getTotalTickets());
        event.setImageUrl(imagePath);
        Event savedEvent = eventRepository.save(event);
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("status", "success");
        mapEvent.put("message", "Event updated successfully.");
        mapEvent.put("data", savedEvent);
        return mapEvent;
    }
}