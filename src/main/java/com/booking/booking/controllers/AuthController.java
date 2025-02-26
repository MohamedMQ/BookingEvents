package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.RegisterUserDto;
import com.booking.booking.dto.LoginUserDto;
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
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



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
    public ResponseEntity<User> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signUp(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticate(@Valid @RequestBody LoginUserDto loginUserDto, 
                                                HttpServletRequest request ,
                                                HttpServletResponse response) {
        User authenticatedUser = authenticationService.signIn(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        ResponseCookie cookie = ResponseCookie
                                .from("accessToken", jwtToken)
                                .httpOnly(true)
                                .secure(false)
                                .path("/")
                                .maxAge(jwtService.getExpirationTime())
                                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Login successful");
        responseBody.put("expiresIn", jwtService.getExpirationTime());
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("")
    public String getMethodName(@RequestParam String param) {
        return new String();
    }
    
}
