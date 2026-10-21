package com.picvi.pet_companion_service.condition.api;

import com.picvi.pet_companion_service.condition.domain.ConditionCertainty;
import com.picvi.pet_companion_service.condition.domain.ConditionKind;
import com.picvi.pet_companion_service.condition.repository.PetConditionRepository;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** What other modules may ask about a pet's conditions. */
@Component
@RequiredArgsConstructor
public class ConditionApi {
    private static final Set<ConditionKind> FOOD_RELATED = Set.of(ConditionKind.ALLERGY, ConditionKind.INTOLERANCE);

    private final PetConditionRepository repository;

    /** A condition as other modules see it. */
    public record ConditionSnapshot(ConditionKind kind, IngredientCode ingredientCode, ConditionCertainty certainty, String text,
                                    LocalDate since) {
    }

    /** Conditions not resolved on the given day, oldest first. */
    @Transactional(readOnly = true)
    public List<ConditionSnapshot> openConditions(UUID petId, LocalDate date) {
        return this.repository.findAllByPetIdOrderByCreatedAtAsc(petId).stream()
                .filter(condition -> condition.isOpenOn(date))
                .map(condition -> new ConditionSnapshot(condition.getKind(),
                        condition.getIngredientCode() == null ? null : IngredientCode.valueOf(condition.getIngredientCode()),
                        condition.getCertainty(), condition.getText(), condition.getSince()))
                .toList();
    }

    /** Ingredients the pet is allergic or intolerant to (suspected or confirmed) and not resolved on the given day. */
    @Transactional(readOnly = true)
    public List<IngredientCode> allergens(UUID petId, LocalDate date) {
        return this.repository.findAllByPetIdOrderByCreatedAtAsc(petId).stream()
                .filter(condition -> FOOD_RELATED.contains(condition.getKind()))
                .filter(condition -> condition.getIngredientCode() != null)
                .filter(condition -> condition.isOpenOn(date))
                .map(condition -> IngredientCode.valueOf(condition.getIngredientCode()))
                .distinct()
                .toList();
    }
}
