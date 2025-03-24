package com.booking.booking.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDto {
    @NotBlank(message = "Enter username")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "Invalid username")
    private String name;

    @NotBlank(message = "Enter email address")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Enter password")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$", message = "Invalid password")
    private String password;
}