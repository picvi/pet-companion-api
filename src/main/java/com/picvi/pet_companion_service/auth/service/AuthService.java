package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.dto.LoginRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.dto.RegisterRequest;
import com.picvi.pet_companion_service.auth.dto.RegisterResponse;
import com.picvi.pet_companion_service.auth.dto.UserResponse;
import com.picvi.pet_companion_service.exception.EmailNotVerified;
import com.picvi.pet_companion_service.exception.InvalidRefreshToken;
import com.picvi.pet_companion_service.exception.InvalidResetCode;
import com.picvi.pet_companion_service.exception.InvalidVerificationCode;
import com.picvi.pet_companion_service.exception.InvalidCredentials;
import com.picvi.pet_companion_service.exception.UserAlreadyExistsException;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    @Qualifier(AttemptLimiterConfig.LOGIN)
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final PasswordPolicy passwordPolicy;

    private volatile String unknownUserHash;

    /** Off only for local debugging (see application.yml); the prod profile forces it on. */
    @Value("${pawstep.auth.email-verification-required:true}")
    private boolean emailVerificationRequired = true;

    /**
     * An email that is already verified gets the same answer as a fresh sign-up, so the endpoint does not reveal who has an account.
     * An unverified one is taken over by the newest sign-up, but only when a new code really goes out, so the credentials
     * and the code in the mailbox always belong to the same request.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = EmailAddress.normalise(request.email());
        this.passwordPolicy.check(request.password(), request.username(), email);
        String passwordHash = this.passwordEncoder.encode(request.password());
        User owner = this.usersRepository.findByEmail(email).orElse(null);
        User sameUsername = this.usersRepository.findByUsername(request.username());

        if (sameUsername != null && (owner == null || !sameUsername.getId().equals(owner.getId()))) {
            throw new UserAlreadyExistsException("Username already exists", "USERNAME_ALREADY_EXISTS");
        }
        if (owner != null) {
            if (!this.emailVerificationRequired) {
                throw new UserAlreadyExistsException("Email already exists", "USER_EMAIL_ALREADY_EXISTS");
            }
            if (!owner.isEmailVerified() && this.emailVerificationService.canSend(owner)) {
                owner.setUsername(request.username());
                owner.setPassword_hash(passwordHash);
                this.emailVerificationService.sendVerification(owner);
            }
            return new RegisterResponse(email, true);
        }

        User user = new User();
        user.setPassword_hash(passwordHash);
        user.setUsername(request.username());
        user.setEmail(email);
        user.setEmailVerified(!this.emailVerificationRequired);

        User saved = this.usersRepository.save(user);
        if (this.emailVerificationRequired) {
            this.emailVerificationService.sendVerification(saved);
        }
        return new RegisterResponse(saved.getEmail(), this.emailVerificationRequired);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        this.loginAttemptLimiter.checkAllowed(request.username());
        User user = this.usersRepository.findByUsername(request.username());

        String hashToCompare = user == null ? this.unknownUserHash() : user.getPassword_hash();
        boolean passwordMatches = this.passwordEncoder.matches(request.password(), hashToCompare);
        if (user == null || !passwordMatches) {
            this.loginAttemptLimiter.recordFailure(request.username());
            throw new InvalidCredentials();
        }
        this.loginAttemptLimiter.recordSuccess(request.username());
        if (!user.isEmailVerified()) {
            throw new EmailNotVerified(user.getEmail());
        }

        return this.buildAuthResponse(user);
    }

    /** A correct code proves the mailbox, so the user gets a session right away. Wrong guesses are counted and kept. */
    @Transactional(noRollbackFor = InvalidVerificationCode.class)
    public LoginResponse verifyEmail(String email, String code) {
        return this.buildAuthResponse(this.emailVerificationService.verify(email, code));
    }

    /** A correct code proves the mailbox, so the user gets a session right away. Wrong guesses are counted and kept. */
    @Transactional(noRollbackFor = InvalidResetCode.class)
    public LoginResponse resetPassword(String email, String code, String newPassword) {
        User user = this.passwordResetService.reset(email, code, newPassword);
        this.loginAttemptLimiter.recordSuccess(user.getUsername());
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

    /** Lets a login for an unknown username spend the same bcrypt time as a real one. */
    private String unknownUserHash() {
        String hash = this.unknownUserHash;
        if (hash == null) {
            hash = this.passwordEncoder.encode(UUID.randomUUID().toString());
            this.unknownUserHash = hash;
        }
        return hash;
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
}
