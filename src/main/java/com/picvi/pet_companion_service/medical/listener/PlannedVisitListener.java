package com.picvi.pet_companion_service.medical.listener;

import com.picvi.pet_companion_service.dayevent.api.DayEventRemoved;
import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** A planned visit lives in her day as an event: removing the event there removes the plan. */
@Component
@RequiredArgsConstructor
public class PlannedVisitListener {
    private final VetVisitRepository repository;

    @EventListener
    public void onDayEventRemoved(DayEventRemoved removed) {
        if (removed.type() != DayEventType.VET) {
            return;
        }
        this.repository.findByPetIdAndDayEventIdAndStatus(removed.petId(), removed.eventId(), VisitStatus.PLANNED)
                .ifPresent(this.repository::delete);
    }
}
