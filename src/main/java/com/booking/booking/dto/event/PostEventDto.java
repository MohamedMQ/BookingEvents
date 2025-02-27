package com.booking.booking.dto.event;

import java.sql.Date;
import java.sql.Time;

import com.booking.booking.utils.StatusEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PostEventDto {

    @NotBlank(message = "name should not be blank or null")
    private String name;
    
    @NotBlank(message = "description should not be blank or null")
    private String description;
    
    @NotBlank(message = "category should not be blank or null")
    private String category;
    
    @NotBlank(message = "date should not be blank or null")
    private Date date;
    
    @NotBlank(message = "time should not be blank or null")
    private Time time;
    
    @NotBlank(message = "location should not be blank or null")
    private String location;
    
    @NotBlank(message = "price should not be blank or null")
    @Size(min = 5, message = "the price must be at least 5$")
    private double price;
    
    @Size(min = 0, message = "the price must be at least 0")
    private long maxTickets;
    
    @NotBlank(message = "status should be either ACTIVE/INACTIVE")
    private StatusEnum status;
}
