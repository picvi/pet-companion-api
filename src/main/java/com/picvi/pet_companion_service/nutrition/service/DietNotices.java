package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.FoodAskPolicy;
import com.picvi.pet_companion_service.nutrition.domain.NoticeCode;
import com.picvi.pet_companion_service.nutrition.domain.PortionState;
import com.picvi.pet_companion_service.nutrition.entity.PetDietNotice;
import com.picvi.pet_companion_service.nutrition.repository.PetDietNoticeRepository;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** The one-time prompts and tips of a pet's diet. */
@Component
@RequiredArgsConstructor
public class DietNotices {
    private final PetDietNoticeRepository repository;
    private final PetLogApi petLogApi;
    private final FoodAskPolicy askPolicy;
    private final Clock clock;

    public Set<NoticeCode> seen(UUID petId) {
        return this.repository.findAllByPetId(petId).stream().map(PetDietNotice::getCode).collect(Collectors.toSet());
    }

    /** ASK or NONE for a pet with no main food. */
    public PortionState askState(UUID petId) {
        List<PetDietNotice> notices = this.repository.findAllByPetId(petId);
        PetDietNotice dismissed = notices.stream().filter(n -> n.getCode() == NoticeCode.FOOD_ASK_DISMISSED).findFirst().orElse(null);
        boolean again = notices.stream().anyMatch(n -> n.getCode() == NoticeCode.FOOD_ASK_DISMISSED_AGAIN);
        long meals = dismissed == null || again ? 0 : this.petLogApi.mealsLoggedSince(petId, dismissed.getSeenAt());
        return this.askPolicy.state(dismissed != null, again, meals);
    }

    /** Records that the owner dismissed a notice. The second dismissal of the food question is stored as "again". */
    public void dismiss(UUID petId, NoticeCode code, LocalDate date) {
        NoticeCode stored = code;
        if (code == NoticeCode.FOOD_ASK_DISMISSED) {
            PortionState state = this.askState(petId);
            boolean first = !this.seen(petId).contains(NoticeCode.FOOD_ASK_DISMISSED);
            if (!first && state != PortionState.ASK) {
                return;
            }
            stored = first ? NoticeCode.FOOD_ASK_DISMISSED : NoticeCode.FOOD_ASK_DISMISSED_AGAIN;
        }
        if (!this.seen(petId).contains(stored)) {
            this.repository.save(new PetDietNotice(petId, stored, date, OffsetDateTime.now(this.clock)));
        }
    }
}
