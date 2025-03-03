package com.booking.booking.dto.event;

import java.time.LocalDateTime;

import com.booking.booking.models.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class singleEventDto {
    private Long id;

    private String name;

    private String description;

    private String location;

    private String category;

    private LocalDateTime eventDateTime;

    private double price;

    private long totalTickets;

    private String imageUrl;

    private Boolean isCancelled;

    private String maker;

    public singleEventDto(Event event) {
        this.id = event.getId();
        this.name = event.getName();
        this.description = event.getDescription();
        this.category = event.getCategory();
        this.eventDateTime = event.getEventDateTime();
        this.location = event.getLocation();
        this.price = event.getPrice();
        this.totalTickets = event.getTotalTickets();
        this.imageUrl = event.getImageUrl();
        this.maker = event.getUser().getName();
    }
}
