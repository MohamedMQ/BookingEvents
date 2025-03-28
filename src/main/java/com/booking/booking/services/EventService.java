package com.booking.booking.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
import org.springframework.web.multipart.MultipartFile;

import com.booking.booking.dto.event.PostEventDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.Ticket;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.repositories.TicketRepository;
import com.booking.booking.utils.TicketStatus;

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
@Service
public class EventService {
    @Value("${spring.web.resources.static-locations}")
    private String fileUploadPath;

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    public EventService(
        EventRepository eventRepository,
        TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    // NOT PROTECTED SERVICES

    public Map<String, Object> getPublicEvents(
        int page,
        int size) {
        Map<String, Object> mapEvent = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> events = eventRepository.findAll(pageable);
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
        Event event = eventRepository
                .findById(eventId)
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
        
        Page<Event> events = eventRepository.findAll(pageable);
        List<Event> eventList = events.stream().map(event -> {
            Set<Ticket> filteredTickets = event.getTickets().stream()
                .filter(ticket -> {
                    System.err.println(ticket.getStatus());
                    return ((ticket.getStatus() == TicketStatus.PENDING
                        || ticket.getStatus() == TicketStatus.QUEUED)
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
                .filter(ticket -> ticket.getStatus() == TicketStatus.CONFIRMED)
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
        Event event = eventRepository
            .findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("No event found with eventId " + eventId));    

        Set<Ticket> filteredTickets = event.getTickets().stream()
            .filter((ticket -> (ticket.getStatus() == TicketStatus.PENDING
                || ticket.getStatus() == TicketStatus.QUEUED)
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
        if (!postEventDto.getEventDateTime().isAfter(LocalDateTime.now()))
            throw new EntityNotFoundException("eventDateTime value is invalid" + postEventDto.getEventDateTime());
        if (postEventDto.getImage() != null && !postEventDto.getImage().isEmpty() && !postEventDto.getImage().getContentType().startsWith("image/"))
            throw new EntityNotFoundException("The file provided is not an image");
        String imagePath = (postEventDto.getImage() != null && !postEventDto.getImage().isEmpty()) ? saveImage(postEventDto.getImage(), user) : getDefaultImagePath();
        Event event = Event
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
                        .user(user)
                        .imageUrl(imagePath)
                        .build();
        event = eventRepository.save(event);
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("status", "success");
        mapEvent.put("message", "Event created successfully.");
        mapEvent.put("data", event);
        return mapEvent;
    }
        
    private String saveImage(MultipartFile image, User user) {
        final String subPath = "users" + separator + "events" + separator + user.getId();
        final String fullPath = fileUploadPath + separator + subPath;
        File targetFolder = new File(fullPath);
        if (!targetFolder.exists()) {
            if (!targetFolder.mkdirs()) {
                // System.out.println("Failed to create the image's containing Folder " + fullPath);
                return null;
            }
        }
        String fileName = image.getOriginalFilename();
        String targetFilePath = targetFolder + separator + System.currentTimeMillis() + ".";
        if (fileName != null && !fileName.isEmpty() && fileName.lastIndexOf(".") != -1)
            targetFilePath += fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        try (InputStream inputStream = new BufferedInputStream(image.getInputStream());
            OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Path.of(targetFilePath)))) {
            byte buffer[] = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return targetFilePath.substring(1);
        } catch (Exception e) {
            System.out.println("Failed to write the image to the destination " + targetFilePath);
        }
        return null;
    }

    private String getDefaultImagePath() {
        final String subPath = "users" + separator + "events";
        final String filePath = fileUploadPath + separator + subPath + separator + "default.jpg";
        return filePath.substring(1);
    }
}