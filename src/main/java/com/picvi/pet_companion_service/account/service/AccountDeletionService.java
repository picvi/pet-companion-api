package com.picvi.pet_companion_service.account.service;

import com.picvi.pet_companion_service.auth.service.RefreshTokenService;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public void delete(UUID userId) {
        if (!this.usersRepository.existsById(userId)) {
            throw new UserDoesNotExist();
        }
        this.nutritionApi.forgetUser(userId);
        int pets = this.petService.deleteAllOwnedBy(userId);
        this.refreshTokenService.deleteAll(userId);
        this.usersRepository.deleteById(userId);
        log.info("Account {} deleted with {} pets", userId, pets);
    }
}
