package com.picvi.pet_companion_service.account.service;

import com.picvi.pet_companion_service.auth.service.AttemptLimiterConfig;
import com.picvi.pet_companion_service.auth.service.LoginAttemptLimiter;
import com.picvi.pet_companion_service.auth.service.RefreshTokenService;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.exception.account.CurrentPasswordIncorrect;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {
    private final UsersRepository usersRepository;
    private final NutritionApi nutritionApi;
    private final PetService petService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    @Qualifier(AttemptLimiterConfig.REAUTH)
    private final LoginAttemptLimiter reauthAttemptLimiter;

    /**
     * Deletes the account only when {@code password} is the user's current password. Wrong attempts share the
     * per-user lock with password change.
     */
    @Transactional
    public void delete(UUID userId, String password) {
        String key = userId.toString();
        this.reauthAttemptLimiter.checkAllowed(key);
        User user = this.usersRepository.findById(userId).orElseThrow(UserDoesNotExist::new);
        if (!this.passwordEncoder.matches(password, user.getPassword_hash())) {
            this.reauthAttemptLimiter.recordFailure(key);
            throw new CurrentPasswordIncorrect();
        }
        this.reauthAttemptLimiter.recordSuccess(key);
        this.nutritionApi.forgetUser(userId);
        int pets = this.petService.deleteAllOwnedBy(userId);
        this.refreshTokenService.deleteAll(userId);
        this.usersRepository.deleteById(userId);
        log.info("Account {} deleted with {} pets", userId, pets);
    }
}
