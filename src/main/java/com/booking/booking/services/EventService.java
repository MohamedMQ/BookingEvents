package com.booking.booking.services;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Service
public class EventService {
    @Value("${application.file.uploads.photos-output-path}")
    private String fileUploadPath;

    EventRepository eventRepository;

    public Map<String, Object> getAllEvents(int page, int size) {
        Map<String, Object> mapEvent = new HashMap<>();
        Map<String, Object> mapPagination = new HashMap<>();
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> events = eventRepository.findAll(pageable, user.getId());
        List<singleEventDto> eventList = events.stream().map((event) -> new singleEventDto(event)).toList();
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

    public void postSingleEvent(PostEventDto postEventDto) {
        if (!postEventDto.getIsCancelled())
            throw new EntityNotFoundException("isCanceled value is invalid" + postEventDto.getIsCancelled());
        if (!postEventDto.getEventDateTime().isAfter(LocalDateTime.now()))
            throw new EntityNotFoundException("eventDateTime value is invalid" + postEventDto.getEventDateTime());
        String imagePath = (postEventDto.getImage() != null && !postEventDto.getImage().isEmpty()) ? saveImage(postEventDto.getImage()) : getDefaultImagePath();
        Event event = new Event(null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
        
    private String saveImage(MultipartFile image) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveImage'");
    }

    private String getDefaultImagePath() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultImagePath'");
    }

     public String saveFile(@Nonnull MultipartFile sourceFile, @Nonnull String userId) {
        final String fileUploadSubPath = "users" + separator + userId;
        return uploadFile(sourceFile, fileUploadSubPath);
    }

    private String uploadFile(@Nonnull MultipartFile sourceFile, @Nonnull String fileUploadSubPath) {
        final String finalUploadPath = fileUploadPath + separator + fileUploadSubPath;
        File targetFolder = new File(finalUploadPath);

        if (!targetFolder.exists()) {
            boolean folderCreated = targetFolder.mkdirs();
            if (!folderCreated) {
                log.warn("Failed to create the target folder: " + targetFolder);
                return null;
            }
        }
        final String fileExtension = getFileExtension(sourceFile.getOriginalFilename());
        String targetFilePath = finalUploadPath + separator + currentTimeMillis() + "." + fileExtension;
        Path targetPath = Paths.get(targetFilePath);
        try {
            Files.write(targetPath, sourceFile.getBytes());
            log.info("File saved to: " + targetFilePath);
            return targetFilePath;
        } catch (IOException e) {
            log.error("File was not saved", e);
        }
        return null;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}