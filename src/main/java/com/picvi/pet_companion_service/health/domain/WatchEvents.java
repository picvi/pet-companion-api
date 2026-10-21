package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.LogPredicates;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WatchEvents {
    private static final Set<LogFlag> WORRY_FLAGS = EnumSet.of(LogFlag.BLOOD, LogFlag.MUCUS, LogFlag.WORMS, LogFlag.BLACK, LogFlag.STRAINING);

    /** Oldest first. Left-food events count only when there are enough of them in the given logs. */
    public List<WatchEvent> from(List<LogSnapshot> logs) {
        List<WatchEvent> events = logs.stream()
                .map(this::eventOf)
                .flatMap(Optional::stream)
                .toList();
        long leftFood = events.stream().filter(event -> event.kind() == WatchKind.LEFT_FOOD).count();
        return events.stream()
                .filter(event -> event.kind() != WatchKind.LEFT_FOOD || leftFood >= HealthLimits.LEFT_FOOD_MIN)
                .sorted(Comparator.comparing(WatchEvent::on).thenComparingInt(WatchEvent::minute))
                .toList();
    }

    public boolean isUnusualPoo(LogSnapshot log) {
        return this.toiletKind(log).isPresent();
    }

    private Optional<WatchEvent> eventOf(LogSnapshot log) {
        return switch (log.type()) {
            case TOILET -> this.toiletKind(log).map(kind -> event(kind, null, BodySign.POO, log));
            case MEAL -> isLeftFood(log) ? Optional.of(event(WatchKind.LEFT_FOOD, null, BodySign.EATING, log)) : Optional.empty();
            case SYMPTOM -> Optional.of(event(WatchKind.SYMPTOM, log.detail(), SymptomSigns.of(log.detail()), log));
            case SLEEP, WAKE, WALK, WEIGHT, MEDICINE, NOTE -> Optional.empty();
        };
    }

    private Optional<WatchKind> toiletKind(LogSnapshot log) {
        if (log.type() != LogType.TOILET) {
            return Optional.empty();
        }
        if (LogPredicates.looseStool(log)) {
            return Optional.of(WatchKind.LOOSE_STOOL);
        }
        boolean worry = log.flags().stream().anyMatch(WORRY_FLAGS::contains);
        return worry ? Optional.of(WatchKind.STOOL_FLAG) : Optional.empty();
    }

    private static boolean isLeftFood(LogSnapshot log) {
        return LogPredicates.leftFood(log);
    }

    private static WatchEvent event(WatchKind kind, LogDetail detail, BodySign sign, LogSnapshot log) {
        return new WatchEvent(kind, detail, sign, log.localDate(), log.minuteOfDay());
    }
}
