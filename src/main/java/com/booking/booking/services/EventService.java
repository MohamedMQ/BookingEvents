package com.booking.booking.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.event.PostEventDto;
import com.booking.booking.dto.event.singleEventDto;
import com.booking.booking.models.Event;
import com.booking.booking.models.User;
import com.booking.booking.repositories.EventRepository;
import com.booking.booking.utils.StatusEnum;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Service
public class EventService {
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
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EntityNotFoundException("No event found with ID:: " + eventId));
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("status", "success");
        mapEvent.put("message", "Data retrieved successfully.");
        mapEvent.put("data", event);
        return mapEvent;
    }

    public void postSingleEvent(PostEventDto postEventDto) {
        if (StatusEnum.valueOf(postEventDto.getStatus()) == null)
            throw new EntityNotFoundException("status value is invalid" + postEventDto.getStatus());
    }
}
