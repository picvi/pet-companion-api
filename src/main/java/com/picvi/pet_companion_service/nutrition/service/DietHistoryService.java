package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.nutrition.domain.ChangeMethod;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinExposureCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionEndReason;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.dto.DietHistoryResponse;
import com.picvi.pet_companion_service.nutrition.dto.HistoryChangeDto;
import com.picvi.pet_companion_service.nutrition.dto.HistoryFoodDto;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.ExposureReader;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DietHistoryService {
    static final int MAX_DAYS = 366;

    private final PetFoodRepository foodRepository;
    private final FoodTransitionRepository transitionRepository;
    private final ExposureReader exposureReader;
    private final ProteinExposureCalculator exposureCalculator;

    public DietHistoryResponse history(UUID petId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new InvalidDate();
        }
        List<PetFood> foods = this.foodRepository.findOverlapping(petId, from, to, PetFoodStatus.ENTERED_IN_ERROR);
        List<HistoryFoodDto> mains = foods.stream().filter(food -> food.getKind() == FoodKind.MAIN).map(this::food).toList();
        List<HistoryFoodDto> others = foods.stream().filter(food -> food.getKind() != FoodKind.MAIN).map(this::food).toList();
        List<HistoryChangeDto> changes = this.transitionRepository.findAllByPetIdOrderByStartOnDesc(petId).stream()
                .filter(transition -> this.overlaps(transition, from, to))
                .sorted(Comparator.comparing(FoodTransition::getStartOn))
                .map(this::change)
                .toList();
        return new DietHistoryResponse(from, to, mains, others, changes,
                this.exposureCalculator.calculate(this.exposureReader.read(petId, from, to)));
    }

    private HistoryFoodDto food(PetFood food) {
        LocalDate last = food.getEndedOn() == null ? null : food.getEndedOn().minusDays(1);
        return new HistoryFoodDto(food.getId(), food.getProduct().getId(), food.getKind(), food.getProduct().getBrand(),
                food.getProduct().getProductName(), food.getProduct().getFlavour(), food.getStartedOn(),
                last != null && last.isBefore(food.getStartedOn()) ? food.getStartedOn() : last, food.getDailyGrams());
    }

    private boolean overlaps(FoodTransition transition, LocalDate from, LocalDate to) {
        LocalDate end = this.lastDay(transition);
        return !transition.getStartOn().isAfter(to) && (end == null || !end.isBefore(from));
    }

    private HistoryChangeDto change(FoodTransition transition) {
        ChangeMethod method = transition.getEndReason() == TransitionEndReason.REPLACED ? ChangeMethod.ALL_AT_ONCE
                : transition.getStatus() == TransitionStatus.CANCELLED ? ChangeMethod.CANCELLED : ChangeMethod.PLANNED;
        return new HistoryChangeDto(transition.getId(), method, transition.getPace(), transition.getFromFoodId(),
                transition.getToFoodId(), transition.getStartOn(), this.lastDay(transition));
    }

    /** DONE changes end on their last day; cancelled ones the day before they were cancelled; an all-at-once switch is one day; running ones have no end. */
    private LocalDate lastDay(FoodTransition transition) {
        if (transition.getEndReason() == TransitionEndReason.REPLACED) {
            return transition.getStartOn();
        }
        if (transition.getStatus() == TransitionStatus.DONE) {
            return transition.getEndedOn();
        }
        if (transition.getStatus() == TransitionStatus.CANCELLED) {
            return transition.getEndedOn().isBefore(transition.getStartOn()) ? transition.getStartOn() : transition.getEndedOn();
        }
        return null;
    }
}
