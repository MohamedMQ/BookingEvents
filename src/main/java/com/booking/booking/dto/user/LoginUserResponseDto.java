package com.booking.booking.dto.user;

import com.booking.booking.models.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserResponseDto {
    private int id;
    private String username;
    private String email;
    public LoginUserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getName();
        this.email = user.getEmail();
    }
}
