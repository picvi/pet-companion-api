package com.picvi.pet_companion_service.account.controller;

import com.picvi.pet_companion_service.account.dto.ChangePasswordRequest;
import com.picvi.pet_companion_service.account.dto.DeleteAccountRequest;
import com.picvi.pet_companion_service.account.service.AccountDeletionService;
import com.picvi.pet_companion_service.account.service.PasswordService;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/me")
public class AccountController {
    private final PasswordService passwordService;
    private final AccountDeletionService accountDeletionService;

    @PostMapping("/password")
    public LoginResponse changePassword(@AuthenticationPrincipal UUID userId,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        return this.passwordService.change(userId, request);
    }

    @PostMapping("/deletion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @Valid @RequestBody DeleteAccountRequest request) {
        this.accountDeletionService.delete(userId, request.password());
    }
}
