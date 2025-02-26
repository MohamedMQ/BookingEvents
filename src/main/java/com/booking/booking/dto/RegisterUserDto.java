package com.booking.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDto {
    @NotBlank(message = "Enter a valid username")
    private String username;

    @Email(message = "Enter a valid email")
    private String email;

    @Size(min = 8, message = "Password should be more than 8 characters")
    private String password;
}