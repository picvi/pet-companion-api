package com.picvi.pet_companion_service.auth.controller;

import com.picvi.pet_companion_service.auth.dto.ForgotPasswordRequest;
import com.picvi.pet_companion_service.auth.dto.LoginRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.dto.RefreshTokenRequest;
import com.picvi.pet_companion_service.auth.dto.RegisterRequest;
import com.picvi.pet_companion_service.auth.dto.RegisterResponse;
import com.picvi.pet_companion_service.auth.dto.ResendVerificationRequest;
import com.picvi.pet_companion_service.auth.dto.ResetPasswordRequest;
import com.picvi.pet_companion_service.auth.dto.VerifyEmailRequest;
import com.picvi.pet_companion_service.auth.dto.UserResponse;
import com.picvi.pet_companion_service.auth.service.AuthService;
import com.picvi.pet_companion_service.auth.service.EmailVerificationService;
import com.picvi.pet_companion_service.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return this.authService.register(request);
    }

    @PostMapping("/verify-email")
    public LoginResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return this.authService.verifyEmail(request.email(), request.code());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        this.emailVerificationService.resend(request.email());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        this.passwordResetService.request(request.email());
    }

    @PostMapping("/reset-password")
    public LoginResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return this.authService.resetPassword(request.email(), request.code(), request.newPassword());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return this.authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return this.authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        this.authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UUID userId) {
        return this.authService.currentUser(userId);
    }
}
