package com.booking.booking.services;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.RegisterUserDto;
import com.booking.booking.dto.LoginUserDto;
import com.booking.booking.models.User;
import com.booking.booking.repositories.UserRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User signUp(RegisterUserDto userDto) {
        Optional<User> optional = userRepository.findByEmail(userDto.getEmail());
        if (optional.isPresent())
            throw new BadCredentialsException("User with this email already exists");
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        return userRepository.save(user);
    }

    public User signIn(LoginUserDto loginUserDto) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDto.getEmail(), loginUserDto.getPassword()));
            System.out.println("getting authenticatedUser");
            return userRepository.findByEmail(loginUserDto.getEmail()).orElseThrow();
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        } catch (NoSuchElementException e) {
            throw new BadCredentialsException("No user with these credentials");
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
}
