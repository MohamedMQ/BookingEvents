package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.user.LoginUserDto;
import com.booking.booking.dto.user.LoginUserResponseDto;
import com.booking.booking.dto.user.RegisterUserDto;
import com.booking.booking.dto.user.RegisterUserResponseDto;
import com.booking.booking.models.User;
import com.booking.booking.services.AuthService;
import com.booking.booking.services.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final AuthService authenticationService;

    public AuthController(JwtService jwtService, AuthService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        Map<String, Object> response = new HashMap<>();
        User registeredUser = authenticationService.signUp(registerUserDto);
        response.put("status", "success");
        response.put("message", "Registered successfully.");
        response.put("data", new RegisterUserResponseDto(registeredUser));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticate(@Valid @RequestBody LoginUserDto loginUserDto, 
                                                HttpServletRequest request ,
                                                HttpServletResponse responseHeader) {
        User authenticatedUser = authenticationService.signIn(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        ResponseCookie cookie = ResponseCookie
                                .from("accessToken", jwtToken)
                                .httpOnly(true)
                                .secure(false)
                                .path("/")
                                .maxAge(jwtService.getExpirationTime())
                                .build();
        responseHeader.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Login successful");
        response.put("data", new LoginUserResponseDto(authenticatedUser));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> profile() {
        User user = authenticationService.getUser();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User details");
        response.put("data", user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
