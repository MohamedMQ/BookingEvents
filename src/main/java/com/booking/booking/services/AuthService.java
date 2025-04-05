package com.booking.booking.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.booking.booking.dto.user.AccountStatusResponseDto;
import com.booking.booking.dto.user.LoginUserDto;
import com.booking.booking.dto.user.LoginUserResponseDto;
import com.booking.booking.dto.user.RegisterUserDto;
import com.booking.booking.models.User;
import com.booking.booking.repositories.UserRepository;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.LoginLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.LoginLinkCreateOnAccountParams;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        User user;
        try {
            AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setEmail(userDto.getEmail())
                .setCountry("US")
                .setCapabilities(
                    AccountCreateParams.Capabilities.builder()
                        .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                            .setRequested(true)
                            .build()
                        )
                        .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                            .setRequested(true)
                            .build()
                        )
                        .build()
                )
                .build();
            Account account = Account.create(params);
            System.out.println("Account ID: " + account.getId());

            if (account.getCapabilities() != null) {
                System.out.println("Transfers capability: " + account.getCapabilities().getTransfers());
                System.out.println("Card payments capability: " + account.getCapabilities().getCardPayments());
                // System.out.println("Platform payments capability: " + account.getCapabilities().getPlatformPayments());
            }
            // Check the account's requirements (verification or other pending actions)
            if (account.getRequirements() != null) {
                List<String> currentlyDue = account.getRequirements().getCurrentlyDue();
                if (currentlyDue != null && !currentlyDue.isEmpty()) {
                    System.out.println("Currently Due Requirements: " + currentlyDue);
                } else {
                    System.out.println("No currently due requirements. The account seems complete.");
                }
            }
            if (account.getRequirements().getDisabledReason() != null) {
                System.out.println("Account is disabled due to: " + account.getRequirements().getDisabledReason());
            } else {
                System.out.println("Account is active and not disabled.");
            }
            AccountLinkCreateParams paramss = AccountLinkCreateParams.builder()
                .setAccount(account.getId())
                .setRefreshUrl("https://yourapp.com/reauth")
                .setReturnUrl("https://yourapp.com/dashboard")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

            AccountLink accountLink = AccountLink.create(paramss);
            System.out.println("ACCOUNT LINK ======> : " + accountLink.getUrl());

            user = new User();
            user.setName(userDto.getName());
            user.setEmail(userDto.getEmail());
            user.setAccountId(account.getId());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        } catch (Exception e) {
            throw new EntityNotFoundException("Something went wrong!! try again later");
        }
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

    public Map<String, Object> getAccountStatus() {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String accountId = user.getAccountId();
        Map<String, Object> res = new HashMap<>();
        try {
            Account account = Account.retrieve(accountId);
            List<String> currentlyDue = account.getRequirements().getCurrentlyDue();
            String disabledReason = account.getRequirements().getDisabledReason();

            String acceptPayments = account.getCapabilities().getTransfers();
            String recievePayout = account.getCapabilities().getCardPayments();

            AccountStatusResponseDto response = new AccountStatusResponseDto();
            response.setCurrentlyDue(currentlyDue);
            response.setDisabledReason(disabledReason);
            response.setAcceptPayments(acceptPayments);
            response.setRecievePayout(recievePayout);

            res.put("status", "success");
            res.put("message", "User details");
            res.put("data", response);
        } catch (Exception e) {
            throw new EntityNotFoundException("Something went wrong when trying retrieving user stripe account");
        }
        return res;
    }

    public Map<String, Object> getAccountLink() {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String accountId = user.getAccountId();
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> accountLinkMap = new HashMap<>();
        try {
            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setReturnUrl("http://localhost:3000/dashboard")
                .setRefreshUrl("http://localhost:3000/dashboard")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();
            AccountLink accountLink = AccountLink.create(params);
            String accountLinkCreate = accountLink.getUrl();
            accountLinkMap.put("accountLink", accountLinkCreate);
            res.put("status", "success");
            res.put("message", "User details");
            res.put("data", accountLinkMap);
        } catch (Exception e) {
            throw new EntityNotFoundException("Something went wrong when trying creating account link");
        }
        return res;
    }

    public Map<String, Object> getDashboardLink() {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String accountId = user.getAccountId();
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> dashboardLinkMap = new HashMap<>();
        try {
            LoginLinkCreateOnAccountParams params =
                LoginLinkCreateOnAccountParams.builder().build();
            LoginLink loginLink = LoginLink.createOnAccount(accountId, params);
            dashboardLinkMap.put("dashboardLink", loginLink.getUrl());
            res.put("status", "success");
            res.put("message", "User details");
            res.put("data", dashboardLinkMap);
        } catch (Exception e) {
            throw new EntityNotFoundException("Something went wrong when trying creating dashboard link");
        }
        return res;
    }

    public Map<String, Object> getProtectedLogout(HttpServletResponse responseHeader) {
        ResponseCookie cookie = ResponseCookie
            .from("accessToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .build();
        responseHeader.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logout successfully");
        return response;
    }
}
