package com.booking.booking.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.RegisterUserDto;
import com.booking.booking.dto.LoginUserDto;
import com.booking.booking.models.User;
import com.booking.booking.repositories.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
// @NoArgsConstructor
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public User signUp(RegisterUserDto userDto) {
        User user = new User().setUsername(userDto.getUsername()).setEmail(userDto.getEmail()).setPassword(passwordEncoder.encode(userDto.getPassword()));
        return UserRepository.save(user);
    }

    public User signIn(LoginUserDto loginUserDto) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDto.getEmail(), loginUserDto.getPassword()));
        return userRepository.findByEmail(loginUserDto.getEmail()).orElseThrow();
    }
}
