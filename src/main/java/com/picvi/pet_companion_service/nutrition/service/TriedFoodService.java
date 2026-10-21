package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.nutrition.SafetyFoodNotFound;
import com.picvi.pet_companion_service.nutrition.domain.TriedFoodRules;
import com.picvi.pet_companion_service.nutrition.dto.TriedFoodDto;
import com.picvi.pet_companion_service.nutrition.dto.TriedFoodRequest;
import com.picvi.pet_companion_service.nutrition.dto.TriedFoodsResponse;
import com.picvi.pet_companion_service.nutrition.entity.FoodSafetyItem;
import com.picvi.pet_companion_service.nutrition.entity.PetTriedFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodSafetyItemRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetTriedFoodRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TriedFoodService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final FoodSafetyItemRepository foodRepository;
    private final PetTriedFoodRepository repository;
    private final TriedFoodRules rules;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TriedFoodsResponse list(UUID ownerId, UUID petId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        return new TriedFoodsResponse(this.repository.findAllByPetIdOrderByTriedOnDescFoodCodeAsc(petId).stream()
                .map(TriedFoodDto::of)
                .toList());
    }

    public TriedFoodDto mark(UUID ownerId, UUID petId, String foodCode, TriedFoodRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        FoodSafetyItem food = this.foodRepository.findById(foodCode).orElseThrow(SafetyFoodNotFound::new);
        String note = blankToNull(request.note());
        this.rules.validate(food.getLevel(), request.triedOn(), note, request.date());

        Instant now = this.clock.instant();
        PetTriedFood tried = this.repository.findByPetIdAndFoodCode(petId, foodCode)
                .map(existing -> {
                    existing.update(request.triedOn(), request.reaction(), note, now);
                    return existing;
                })
                .orElseGet(() -> new PetTriedFood(petId, foodCode, request.triedOn(), request.reaction(), note, ownerId, now));
        return TriedFoodDto.of(this.repository.saveAndFlush(tried));
    }

    public void remove(UUID ownerId, UUID petId, String foodCode) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        this.repository.findByPetIdAndFoodCode(petId, foodCode).ifPresent(this.repository::delete);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
