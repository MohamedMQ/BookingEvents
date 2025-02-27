package com.booking.booking.dto.user;

import com.booking.booking.models.User;

public class RegisterUserResponseDto {
    private int id;
    private String username;
    private String email;
    public RegisterUserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getName();
        this.email = user.getEmail();
    }
}
