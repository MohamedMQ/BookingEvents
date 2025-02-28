package com.booking.booking.dto.event;

import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PostEventDto {

    @NotBlank(message = "Name should not be blank or null")
    private String name;
    
    @NotBlank(message = "Description should not be blank or null")
    private String description;

    @NotBlank(message = "Location should not be blank or null")
    private String location;
    
    @NotBlank(message = "Category should not be blank or null")
    private String category;
    
    @NotNull(message = "Event date and time should not be null")
    private LocalDateTime eventDateTime;

    @NotNull(message = "Price should not be blank or null")
    @Min(value = 5, message = "The price must be at least 5$")
    private double price;
    
    @NotNull(message = "Total tickets should not be blank or null")
    @Min(value = 1, message = "The total tickets must be at least 1")
    private long totalTickets;
    
    @NotNull(message = "Status should be either true or false")
    private Boolean isCancelled;

    private MultipartFile image;
}
