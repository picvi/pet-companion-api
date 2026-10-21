package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.petlog.api.PetLogSaved;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

/** Reacts to a saved log after it is committed, in a transaction of its own. A failure here never fails the log. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TummyReactionListener {
    private static final Set<LogType> RELEVANT = Set.of(LogType.TOILET, LogType.SYMPTOM, LogType.MEAL);

    private final TummyReactionService reactions;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLogSaved(PetLogSaved event) {
        if (!RELEVANT.contains(event.type())) {
            return;
        }
        try {
            this.reactions.apply(event.petId(), LocalDate.now(this.clock));
        } catch (RuntimeException e) {
            log.warn("Tummy reaction for pet {} failed; the next diet read applies it", event.petId(), e);
        }
    }
}
