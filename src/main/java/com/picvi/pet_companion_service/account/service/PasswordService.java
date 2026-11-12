package com.picvi.pet_companion_service.account.service;

import com.picvi.pet_companion_service.account.dto.ChangePasswordRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.service.AuthService;
import com.picvi.pet_companion_service.auth.service.PasswordPolicy;
import com.picvi.pet_companion_service.auth.service.RefreshTokenService;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.exception.account.CurrentPasswordIncorrect;
import com.picvi.pet_companion_service.exception.account.PasswordUnchanged;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final PasswordPolicy passwordPolicy;

    @Transactional
    public LoginResponse change(UUID userId, ChangePasswordRequest request) {
        User user = this.usersRepository.findById(userId).orElseThrow(UserDoesNotExist::new);
        if (!this.passwordEncoder.matches(request.currentPassword(), user.getPassword_hash())) {
            throw new CurrentPasswordIncorrect();
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new PasswordUnchanged();
        }
        this.passwordPolicy.check(request.newPassword(), user.getUsername(), user.getEmail());
        user.setPassword_hash(this.passwordEncoder.encode(request.newPassword()));
        this.refreshTokenService.revokeAll(userId);
        return this.authService.issueSession(user);
    }
}
