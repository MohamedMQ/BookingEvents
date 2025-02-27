package com.booking.booking.dto.event;

import java.sql.Date;
import java.sql.Time;

import com.booking.booking.models.Event;
import com.booking.booking.utils.StatusEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class singleEventDto {
    private int id;

    private String name;

    private String description;

    private String category;

    private Date date;

    private Time time;

    private String location;

    private double price;

    private long maxTickets;

    private String imageUrl;

    private StatusEnum status;

    private String maker;

    public singleEventDto(Event event) {
        this.id = event.getId();
        this.name = event.getName();
        this.description = event.getDescription();
        this.category = event.getCategory();
        this.date = event.getDate();
        this.time = event.getTime();
        this.location = event.getLocation();
        this.price = event.getPrice();
        this.maxTickets = event.getMaxTickets();
        this.imageUrl = event.getImageUrl();
        this.status = event.getStatus();
        this.maker = event.getUser().getName();
    }
}
