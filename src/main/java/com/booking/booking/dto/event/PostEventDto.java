package com.booking.booking.dto.event;

import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.booking.booking.utils.MusicCategory;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PostEventDto {

    @NotBlank(message = "Enter Event Name")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9 -]{3,48}[a-zA-Z0-9]$", message = "Invalid Event Name")
    private String name;
    
    @NotBlank(message = "Enter Description")
    @Pattern(regexp = "^[\\w\\d\\s.,!?&'\\\"()-]{20,500}$", message = "Invalid Description")
    private String description;

    @NotBlank(message = "Enter Location")
    @Pattern(regexp = "^[a-zA-Z0-9\\s.,'-]{5,100}$", message = "Invalid Location")
    private String location;

    @NotNull(message = "Enter Category")
    private MusicCategory category;
    
    @NotNull(message = "Enter event date")
    private LocalDateTime eventDateTime;

    @NotNull(message = "Enter Price Per Ticket")
    @DecimalMin(value = "5", inclusive = true, message = "Invalid Price Per Ticket")
    @DecimalMax(value = "9999.99", inclusive = true, message = "Invalid Price Per Ticket")
    private Double price;
    
    @NotNull(message = "Enter Price Per Ticket")
    @Min(value = 1, message = "invalid Total Tickets Available") // VALUE SHOULD BE 5
    @Max(value = 9999, message = "invalid Total Tickets Available")
    private Integer totalTickets;

    private MultipartFile image;
}
