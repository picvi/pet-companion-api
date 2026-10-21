package com.picvi.pet_companion_service.triage.api;

import com.picvi.pet_companion_service.triage.domain.CheckCatalog;
import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.domain.Level;
import com.picvi.pet_companion_service.triage.entity.PetCheck;
import com.picvi.pet_companion_service.triage.repository.PetCheckRepository;
import com.picvi.pet_companion_service.triage.service.CatalogLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TriageApi {
    private final PetCheckRepository checkRepository;
    private final CatalogLoader catalogLoader;

    public record CheckSnapshot(UUID id, String item, String kind, String category, Level level, CheckStatus status,
                                LocalDate occurredOn, int minuteOfDay, LocalDate resolvedOn, String reviewStatus) {
    }

    @Transactional(readOnly = true)
    public List<CheckSnapshot> checksBetween(UUID petId, LocalDate from, LocalDate to) {
        return this.checkRepository.findAllByPetIdAndOccurredOnBetweenOrderByOccurredOnAscMinuteOfDayAsc(petId, from, to)
                .stream().map(this::snapshot).toList();
    }

    private CheckSnapshot snapshot(PetCheck check) {
        CheckCatalog.Item item = this.catalogLoader.getCatalog().item(check.getItem());
        String kind = item == null ? "SYMPTOM" : item.kind();
        Object reviewValue = check.getResult().get("review");
        Object reviewStatus = reviewValue instanceof java.util.Map<?, ?> review ? review.get("status") : "PENDING";
        return new CheckSnapshot(check.getId(), check.getItem(), kind, check.getCategory(), check.getLevel(),
                check.getStatus(), check.getOccurredOn(), check.getMinuteOfDay(), check.getResolvedOn(),
                reviewStatus == null ? "PENDING" : String.valueOf(reviewStatus));
    }
}
