package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.dto.LoginRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.dto.RegisterRequest;
import com.picvi.pet_companion_service.auth.dto.UserResponse;
import com.picvi.pet_companion_service.exception.InvalidRefreshToken;
import com.picvi.pet_companion_service.exception.PasswordIncorrect;
import com.picvi.pet_companion_service.exception.UserAlreadyExistsException;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final PetRepository petRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        this.validateUsernameAndEmailAvailable(request.username(), request.email());

        User user = new User();
        user.setPassword_hash(this.passwordEncoder.encode(request.password()));
        user.setUsername(request.username());
        user.setEmail(request.email());

        return this.buildAuthResponse(this.usersRepository.save(user));
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = this.usersRepository.findByUsername(request.username());

        if (user == null) {
            throw new UserDoesNotExist();
        }
        if (!this.passwordEncoder.matches(request.password(), user.getPassword_hash())) {
            throw new PasswordIncorrect();
        }

        return this.buildAuthResponse(user);
    }

    @Transactional(noRollbackFor = InvalidRefreshToken.class)
    public LoginResponse refresh(String refreshToken) {
        User user = this.refreshTokenService.rotate(refreshToken);
        return this.buildAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        this.refreshTokenService.revoke(refreshToken);
    }

    public UserResponse currentUser(UUID userId) {
        User user = this.usersRepository.findById(userId).orElseThrow(UserDoesNotExist::new);
        return this.toUserResponse(user);
    }

    public LoginResponse issueSession(User user) {
        return this.buildAuthResponse(user);
    }

    private LoginResponse buildAuthResponse(User user) {
        return new LoginResponse(
                this.jwtService.generateToken(user),
                this.refreshTokenService.issue(user),
                this.toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        boolean hasPet = this.petRepository.existsByOwnerId(user.getId());
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), hasPet);
    }

    private void validateUsernameAndEmailAvailable(String userName, String email) {
        if (this.usersRepository.existsByUsername(userName)) {
            throw new UserAlreadyExistsException("Username already exists", "USERNAME_ALREADY_EXISTS");
        }

        if (this.usersRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already exists", "USER_EMAIL_ALREADY_EXISTS");
        }
    }
}
