package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.nutrition.FoodNotTriable;
import com.picvi.pet_companion_service.exception.nutrition.SafetyFoodNotFound;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.nutrition.domain.SafetyLevel;
import com.picvi.pet_companion_service.nutrition.domain.TriedFoodRules;
import com.picvi.pet_companion_service.nutrition.domain.TriedReaction;
import com.picvi.pet_companion_service.nutrition.dto.TriedFoodDto;
import com.picvi.pet_companion_service.nutrition.dto.TriedFoodRequest;
import com.picvi.pet_companion_service.nutrition.entity.FoodSafetyItem;
import com.picvi.pet_companion_service.nutrition.entity.PetTriedFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodSafetyItemRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetTriedFoodRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriedFoodServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private FoodSafetyItemRepository foodRepository;
    @Mock
    private PetTriedFoodRepository repository;

    private TriedFoodService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new TriedFoodService(this.clientDateValidator, this.petService, this.foodRepository, this.repository,
                new TriedFoodRules(), clock);
    }

    @Test
    void marksAFoodAsTriedTheFirstTime() {
        when(this.foodRepository.findById("CARROT")).thenReturn(Optional.of(food("CARROT", SafetyLevel.YES)));
        when(this.repository.findByPetIdAndFoodCode(PET, "CARROT")).thenReturn(Optional.empty());
        when(this.repository.saveAndFlush(any(PetTriedFood.class))).thenAnswer(call -> call.getArgument(0));

        TriedFoodDto dto = this.service.mark(OWNER, PET, "CARROT", new TriedFoodRequest(TODAY, TODAY, TriedReaction.LOVED_IT, "  crunchy "));

        assertThat(dto.foodCode()).isEqualTo("CARROT");
        assertThat(dto.reaction()).isEqualTo(TriedReaction.LOVED_IT);
        assertThat(dto.note()).isEqualTo("crunchy");
    }

    @Test
    void markingAgainOverwritesTheEarlierEntry() {
        PetTriedFood existing = new PetTriedFood(PET, "CARROT", TODAY.minusDays(5), TriedReaction.FINE, null, OWNER,
                Instant.parse("2026-10-15T10:00:00Z"));
        when(this.foodRepository.findById("CARROT")).thenReturn(Optional.of(food("CARROT", SafetyLevel.YES)));
        when(this.repository.findByPetIdAndFoodCode(PET, "CARROT")).thenReturn(Optional.of(existing));
        when(this.repository.saveAndFlush(any(PetTriedFood.class))).thenAnswer(call -> call.getArgument(0));

        TriedFoodDto dto = this.service.mark(OWNER, PET, "CARROT", new TriedFoodRequest(TODAY, TODAY.minusDays(5), TriedReaction.UPSET_TUMMY, "   "));

        assertThat(dto.reaction()).isEqualTo(TriedReaction.UPSET_TUMMY);
        assertThat(dto.note()).isNull();
        assertThat(existing.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-20T10:00:00Z"));
        assertThat(existing.getCreatedAt()).isEqualTo(Instant.parse("2026-10-15T10:00:00Z"));
    }

    @Test
    void aNeverFoodIsRefusedAndNothingIsSaved() {
        when(this.foodRepository.findById("GRAPES")).thenReturn(Optional.of(food("GRAPES", SafetyLevel.NEVER)));

        assertThatThrownBy(() -> this.service.mark(OWNER, PET, "GRAPES", new TriedFoodRequest(TODAY, TODAY, TriedReaction.FINE, null)))
                .isInstanceOf(FoodNotTriable.class);
        verify(this.repository, never()).saveAndFlush(any());
    }

    @Test
    void anUnknownFoodIsNotFound() {
        when(this.foodRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.mark(OWNER, PET, "NOPE", new TriedFoodRequest(TODAY, TODAY, TriedReaction.FINE, null)))
                .isInstanceOf(SafetyFoodNotFound.class);
    }

    @Test
    void anotherOwnersPetIsRefusedBeforeAnythingIsRead() {
        when(this.petService.getOwnedPetProfile(OWNER, PET)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.mark(OWNER, PET, "CARROT", new TriedFoodRequest(TODAY, TODAY, TriedReaction.FINE, null)))
                .isInstanceOf(PetNotFound.class);
        assertThatThrownBy(() -> this.service.list(OWNER, PET)).isInstanceOf(PetNotFound.class);
        assertThatThrownBy(() -> this.service.remove(OWNER, PET, "CARROT")).isInstanceOf(PetNotFound.class);
        verify(this.repository, never()).delete(any());
    }

    @Test
    void removingSomethingNeverTriedIsHarmless() {
        when(this.repository.findByPetIdAndFoodCode(PET, "CARROT")).thenReturn(Optional.empty());

        this.service.remove(OWNER, PET, "CARROT");

        verify(this.repository, never()).delete(any());
    }

    private static FoodSafetyItem food(String code, SafetyLevel level) {
        FoodSafetyItem item = new FoodSafetyItem();
        ReflectionTestUtils.setField(item, "code", code);
        ReflectionTestUtils.setField(item, "level", level);
        return item;
    }
}
