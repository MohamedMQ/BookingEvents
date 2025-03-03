package com.booking.booking.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.descriptor.web.ContextHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.booking.booking.dto.event.PostEventDto;
import com.booking.booking.dto.event.singleEventDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    @Value("${application.file.uploads.photos-output-path}")
    private String fileUploadPath;

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Map<String, Object> getAllEvents(int page, int size) {
        Map<String, Object> mapEvent = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> events = eventRepository.findAll(pageable);
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
    
    public Map<String, Object> getSingleEvent(Integer eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EntityNotFoundException("No event found with ID " + eventId));
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", event);
        return mapEvent;
    }

    public Map<String, Object> postSingleEvent(PostEventDto postEventDto) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!postEventDto.getEventDateTime().isAfter(LocalDateTime.now()))
            throw new EntityNotFoundException("eventDateTime value is invalid" + postEventDto.getEventDateTime());
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
        final String subPath = "users" + separator + user.getId();
        final String fullPath = fileUploadPath + separator + subPath;
        File targetFolder = new File(fullPath);
        if (!targetFolder.exists()) {
            if (targetFolder.mkdirs()) {
                System.out.println("Failed to create the image's containing Folder " + fullPath);
                return null;
            }
        }
        String fileName = image.getOriginalFilename();
        String targetFilePath = targetFolder + separator + System.currentTimeMillis() + ".";
        if (fileName != null && !fileName.isEmpty() && fileName.lastIndexOf(".") != -1)
            targetFilePath += fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        try (InputStream inputStream = new BufferedInputStream(image.getInputStream());
            OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Path.of(targetFilePath)))) {
            byte buffer[] = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return targetFilePath;
        } catch (Exception e) {
            System.out.println("Failed to write the image to the destination " + targetFilePath);        }
        return null;
    }

    private String getDefaultImagePath() {
        String filePath = fileUploadPath + separator + "default.jpg";
        return filePath;
    }

    public Map<String, Object> searchEvents(int page, int size, String searchTerm) {
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
}