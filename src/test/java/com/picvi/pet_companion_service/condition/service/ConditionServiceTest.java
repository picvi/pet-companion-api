package com.picvi.pet_companion_service.condition.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.condition.domain.ConditionCertainty;
import com.picvi.pet_companion_service.condition.domain.ConditionKind;
import com.picvi.pet_companion_service.condition.domain.ConditionRules;
import com.picvi.pet_companion_service.condition.dto.ConditionDto;
import com.picvi.pet_companion_service.condition.dto.ConditionRequest;
import com.picvi.pet_companion_service.condition.entity.PetCondition;
import com.picvi.pet_companion_service.condition.repository.PetConditionRepository;
import com.picvi.pet_companion_service.exception.condition.ConditionNotFound;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private PetConditionRepository repository;

    private ConditionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new ConditionService(this.clientDateValidator, this.petService, this.repository, new ConditionRules(), clock);
    }

    @Test
    void addsASuspectedAllergyByDefault() {
        when(this.repository.saveAndFlush(any(PetCondition.class))).thenAnswer(call -> call.getArgument(0));

        ConditionDto dto = this.service.add(OWNER, PET, new ConditionRequest(ConditionKind.ALLERGY, IngredientCode.CHICKEN, null, null, null, null, TODAY));

        assertThat(dto.certainty()).isEqualTo(ConditionCertainty.SUSPECTED);
        assertThat(dto.ingredientCode()).isEqualTo(IngredientCode.CHICKEN);
    }

    @Test
    void updateResolvesAnAllergy() {
        PetCondition condition = new PetCondition(PET, ConditionKind.ALLERGY, "CHICKEN", ConditionCertainty.SUSPECTED, null, null, OWNER, Instant.now());
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(condition));
        when(this.repository.saveAndFlush(condition)).thenReturn(condition);

        ConditionDto dto = this.service.update(OWNER, PET, id, 0, new ConditionRequest(ConditionKind.ALLERGY, IngredientCode.CHICKEN,
                ConditionCertainty.VET_CONFIRMED, null, null, TODAY, TODAY));

        assertThat(dto.certainty()).isEqualTo(ConditionCertainty.VET_CONFIRMED);
        assertThat(dto.resolvedOn()).isEqualTo(TODAY);
    }

    @Test
    void aStaleVersionIsRejected() {
        PetCondition condition = new PetCondition(PET, ConditionKind.ALLERGY, "CHICKEN", ConditionCertainty.SUSPECTED, null, null, OWNER, Instant.now());
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(condition));

        assertThatThrownBy(() -> this.service.update(OWNER, PET, id, 5, new ConditionRequest(ConditionKind.ALLERGY, IngredientCode.CHICKEN, null, null, null, null, TODAY)))
                .isInstanceOf(VersionConflict.class);
    }

    @Test
    void anUnknownConditionIsNotFound() {
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.update(OWNER, PET, id, 0, new ConditionRequest(ConditionKind.ALLERGY, IngredientCode.CHICKEN, null, null, null, null, TODAY)))
                .isInstanceOf(ConditionNotFound.class);
    }

    @Test
    void aResolvedAllergyNoLongerCounts() {
        PetCondition condition = new PetCondition(PET, ConditionKind.ALLERGY, "CHICKEN", ConditionCertainty.SUSPECTED, null, null, OWNER, Instant.now());
        condition.update(ConditionKind.ALLERGY, "CHICKEN", ConditionCertainty.SUSPECTED, null, null, TODAY);

        assertThat(condition.isOpenOn(TODAY.minusDays(1))).isTrue();
        assertThat(condition.isOpenOn(TODAY)).isFalse();
    }

    @Test
    void deleteRemovesAnExistingCondition() {
        PetCondition condition = new PetCondition(PET, ConditionKind.ALLERGY, "CHICKEN", ConditionCertainty.SUSPECTED, null, null, OWNER, Instant.now());
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(condition));

        this.service.delete(OWNER, PET, id);

        verify(this.repository).delete(condition);
    }

    @Test
    void deleteOfAMissingConditionDoesNothing() {
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.empty());

        this.service.delete(OWNER, PET, id);

        verify(this.repository, never()).delete(any());
    }

    @Test
    void deleteOfAnotherUsersPetIsNotFoundAndTouchesNothing() {
        when(this.petService.getOwnedPetProfile(OWNER, PET)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.delete(OWNER, PET, UUID.randomUUID())).isInstanceOf(PetNotFound.class);
        verifyNoInteractions(this.repository);
    }
}
