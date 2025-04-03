package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.dto.user.AccountStatusResponseDto;
import com.booking.booking.dto.user.LoginUserDto;
import com.booking.booking.dto.user.LoginUserResponseDto;
import com.booking.booking.dto.user.RegisterUserDto;
import com.booking.booking.dto.user.RegisterUserResponseDto;
import com.booking.booking.models.User;
import com.booking.booking.services.AuthService;
import com.booking.booking.services.JwtService;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtService jwtService;
    private final AuthService authenticationService;

    /* NOT PROTECTED ROUTES */

    @PostMapping("/public/auth/register")
    public ResponseEntity<Map<String, Object>> publicRegister(@Valid @RequestBody RegisterUserDto registerUserDto) {
        Map<String, Object> response = new HashMap<>();
        User registeredUser = authenticationService.postPublicSignUp(registerUserDto);
        response.put("status", "success");
        response.put("message", "Registered successfully.");
        response.put("data", new RegisterUserResponseDto(registeredUser));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/public/auth/login")
    public ResponseEntity<Map<String, Object>> publicLogin(@Valid @RequestBody LoginUserDto loginUserDto, 
                                                HttpServletRequest request ,
                                                HttpServletResponse responseHeader) {
        User authenticatedUser = authenticationService.postPublicLogin(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        ResponseCookie cookie = ResponseCookie
                                .from("accessToken", jwtToken)
                                .httpOnly(true)
                                .secure(true)
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

    /* PROTECTED ROUTES */

    @GetMapping("/protected/auth/profile")
    public ResponseEntity<Map<String, Object>> protectedProfile() {
        User user = authenticationService.getProtectedProfile();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User details");
        response.put("data", user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/protected/account/status")
    public ResponseEntity<Map<String, Object>> accountStatus() {
        Map<String, Object> res = authenticationService.getAccountStatus();
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/protected/account/link")
    public ResponseEntity<Map<String, Object>> accountLink() {
        Map<String, Object> res = authenticationService.getAccountLink();
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/protected/dashboard/link")
    public ResponseEntity<Map<String, Object>> dashboardLink() {
        Map<String, Object> res = authenticationService.getDashboardLink();
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    // Update User Info Coming Soon
}
