package com.booking.booking.services;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.user.LoginUserDto;
import com.booking.booking.dto.user.RegisterUserDto;
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

    public User postPublicSignUp(RegisterUserDto userDto) {
        Optional<User> optional = userRepository.findByEmail(userDto.getEmail());
        if (optional.isPresent())
            throw new BadCredentialsException("User with this email already exists");
        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        return userRepository.save(user);
    }

    public User postPublicLogin(LoginUserDto loginUserDto) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDto.getEmail(), loginUserDto.getPassword()));
            return userRepository.findByEmail(loginUserDto.getEmail()).orElseThrow();
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        } catch (NoSuchElementException e) {
            throw new BadCredentialsException("No user with these credentials");
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    public User getProtectedProfile() {
        UserDetails userDetails = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
