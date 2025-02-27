package com.booking.booking.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserDto {
    @Email(message = "Enter a valid email")
    private String email;

    @Size(min = 8, message = "Password should be more than 8 characters")
    private String password;
}
