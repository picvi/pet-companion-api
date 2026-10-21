package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.dto.LoginRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.dto.RegisterRequest;
import com.picvi.pet_companion_service.exception.PasswordIncorrect;
import com.picvi.pet_companion_service.exception.UserAlreadyExistsException;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerStoresHashedPasswordAndReturnsTokens() {
        when(usersRepository.existsByUsername("viki")).thenReturn(false);
        when(usersRepository.existsByEmail("viki@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(usersRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(jwtService.generateToken(any(User.class))).thenReturn("access");
        when(refreshTokenService.issue(any(User.class))).thenReturn("refresh");
        when(petRepository.existsByOwnerId(USER_ID)).thenReturn(false);

        LoginResponse response = authService.register(new RegisterRequest("viki", "viki@example.com", "secret123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().id()).isEqualTo(USER_ID);
        assertThat(response.user().hasPet()).isFalse();
    }

    @Test
    void registerRejectsTakenUsername() {
        when(usersRepository.existsByUsername("viki")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("viki", "viki@example.com", "secret123")))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Username already exists");
        verify(usersRepository, never()).save(any(User.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = existingUser();
        when(usersRepository.findByUsername("viki")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("viki", "wrong")))
                .isInstanceOf(PasswordIncorrect.class);
        verify(refreshTokenService, never()).issue(any(User.class));
    }

    @Test
    void loginRejectsUnknownUser() {
        when(usersRepository.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "secret123")))
                .isInstanceOf(UserDoesNotExist.class);
    }

    @Test
    void loginReturnsTokensForValidCredentials() {
        User user = existingUser();
        when(usersRepository.findByUsername("viki")).thenReturn(user);
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");
        when(petRepository.existsByOwnerId(USER_ID)).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("viki", "secret123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().hasPet()).isTrue();
    }

    private User existingUser() {
        User user = new User();
        user.setUsername("viki");
        user.setEmail("viki@example.com");
        user.setPassword_hash("hash");
        return withId(user);
    }

    private static User withId(User user) {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
