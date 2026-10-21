package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.health.domain.HealthSummary.Headline;
import com.picvi.pet_companion_service.health.domain.HealthSummary.Note;
import com.picvi.pet_companion_service.health.domain.HealthSummary.SignSummary;
import com.picvi.pet_companion_service.health.domain.HealthSummary.SignWatch;
import com.picvi.pet_companion_service.nutrition.api.FoodChangeNow;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class HealthSummaryRule {
    private final WatchEvents watchEvents;

    public HealthSummary summarise(LocalDate date, LocalDate petAddedOn, List<LogSnapshot> allLogs, Optional<FoodChangeNow> foodChange) {
        return summarise(date, petAddedOn, allLogs, foodChange, List.of());
    }

    public HealthSummary summarise(LocalDate date, LocalDate petAddedOn, List<LogSnapshot> allLogs,
                                   Optional<FoodChangeNow> foodChange, List<WatchEvent> triageEvents) {
        LocalDate windowStart = date.minusDays(HealthLimits.WATCH_DAYS - 1L);
        List<LogSnapshot> logs = allLogs.stream().filter(log -> !log.localDate().isBefore(windowStart)).toList();
        LocalDate weekStart = date.minusDays(HealthLimits.SIGN_DAYS - 1L);
        List<LogSnapshot> week = logs.stream().filter(log -> !log.localDate().isBefore(weekStart)).toList();
        List<WatchEvent> events = java.util.stream.Stream.concat(this.watchEvents.from(logs).stream(),
                triageEvents.stream().filter(event -> !event.on().isBefore(windowStart) && !event.on().isAfter(date)))
                .sorted(Comparator.comparing(WatchEvent::on).thenComparingInt(WatchEvent::minute)).toList();
        List<WatchEvent> weekEvents = events.stream().filter(event -> !event.on().isBefore(weekStart)).toList();

        CardState state = this.cardState(date, petAddedOn, logs, week, events);
        Headline headline = state == CardState.WATCH ? this.headline(events) : null;
        List<SignSummary> signs = this.signs(date, week, weekEvents);
        Note note = this.note(state, headline, date, logs, events, foodChange);
        return new HealthSummary(state, headline, note, signs, foodChange.orElse(null));
    }

    private CardState cardState(LocalDate date, LocalDate petAddedOn, List<LogSnapshot> logs, List<LogSnapshot> week, List<WatchEvent> events) {
        if (!events.isEmpty()) {
            return CardState.WATCH;
        }
        if (!week.isEmpty()) {
            return CardState.USUAL;
        }
        boolean justArrived = date.isBefore(petAddedOn.plusDays(HealthLimits.SETTLING_DAYS)) && logs.isEmpty();
        return justArrived ? CardState.SETTLING_IN : CardState.QUIET;
    }

    private Headline headline(List<WatchEvent> events) {
        WatchEvent latest = latest(events);
        List<WatchEvent> same = sameKind(events, latest);
        return new Headline(latest.sign(), latest.kind(), latest.detail(), same.size(), same.getFirst().on(), same.getLast().on());
    }

    private List<SignSummary> signs(LocalDate date, List<LogSnapshot> week, List<WatchEvent> weekEvents) {
        Set<BodySign> watched = weekEvents.stream().map(WatchEvent::sign).collect(Collectors.toSet());
        return Arrays.stream(BodySign.values())
                .filter(sign -> BodySign.always().contains(sign) || watched.contains(sign))
                .map(sign -> this.sign(sign, date, week, weekEvents, watched.contains(sign)))
                .toList();
    }

    private SignSummary sign(BodySign sign, LocalDate date, List<LogSnapshot> week, List<WatchEvent> weekEvents, boolean watched) {
        if (watched) {
            return new SignSummary(sign, SignState.WATCH, this.watch(sign, date, week, weekEvents));
        }
        boolean hasData = switch (sign) {
            case EATING -> week.stream().anyMatch(log -> log.type() == LogType.MEAL);
            case POO -> week.stream().anyMatch(log -> log.type() == LogType.TOILET && isPoo(log));
            case SLEEP -> week.stream().anyMatch(log -> log.type() == LogType.SLEEP || log.type() == LogType.WAKE);
            case ENERGY, VOMIT, SKIN, DRINKING, BREATHING, OTHER -> !week.isEmpty();
        };
        return new SignSummary(sign, hasData ? SignState.USUAL : SignState.NO_DATA, null);
    }

    private SignWatch watch(BodySign sign, LocalDate date, List<LogSnapshot> week, List<WatchEvent> weekEvents) {
        List<WatchEvent> own = weekEvents.stream().filter(event -> event.sign() == sign).toList();
        WatchEvent latest = latest(own);
        List<WatchEvent> same = sameKind(own, latest);
        LocalDate to = same.getLast().on();
        return new SignWatch(latest.kind(), latest.detail(), same.size(), same.getFirst().on(), to, latest.on(), latest.minute(),
                this.normalSince(sign, to, date, week));
    }

    private LocalDate normalSince(BodySign sign, LocalDate after, LocalDate date, List<LogSnapshot> week) {
        if (sign != BodySign.POO && sign != BodySign.EATING) {
            return null;
        }
        return this.usualDays(sign, week).stream().filter(day -> day.isAfter(after) && !day.isAfter(date)).min(Comparator.naturalOrder()).orElse(null);
    }

    private Note note(CardState state, Headline headline, LocalDate date, List<LogSnapshot> logs, List<WatchEvent> events,
                      Optional<FoodChangeNow> foodChange) {
        Optional<FoodChangeNow.State> change = foodChange.map(FoodChangeNow::state);
        return switch (state) {
            case SETTLING_IN -> new Note(NoteCode.SETTLING_IN, null);
            case QUIET -> new Note(NoteCode.QUIET, null);
            case USUAL -> new Note(change.filter(FoodChangeNow.State.RUNNING::equals).isPresent() ? NoteCode.FOOD_CHANGE : NoteCode.NO_SYMPTOMS, null);
            case WATCH -> this.watchNote(headline, date, logs, events, change);
        };
    }

    private Note watchNote(Headline headline, LocalDate date, List<LogSnapshot> logs, List<WatchEvent> events,
                           Optional<FoodChangeNow.State> change) {
        if (change.filter(FoodChangeNow.State.PAUSED::equals).isPresent()) {
            return new Note(NoteCode.FOOD_PAUSED, null);
        }
        if (this.gettingBetter(headline.sign(), date, logs, events)) {
            return new Note(NoteCode.GETTING_BETTER, headline.sign());
        }
        return new Note(NoteCode.CALL_IF_WORSE, null);
    }

    private boolean gettingBetter(BodySign sign, LocalDate date, List<LogSnapshot> logs, List<WatchEvent> events) {
        LocalDate yesterday = date.minusDays(1);
        boolean calmSince = events.stream().noneMatch(event -> !event.on().isBefore(yesterday));
        Set<LocalDate> usual = this.usualDays(sign, logs);
        return calmSince && usual.contains(date) && usual.contains(yesterday);
    }

    private Set<LocalDate> usualDays(BodySign sign, List<LogSnapshot> logs) {
        return logs.stream()
                .filter(log -> switch (sign) {
                    case POO -> log.type() == LogType.TOILET && isPoo(log) && !this.watchEvents.isUnusualPoo(log);
                    case EATING -> log.type() == LogType.MEAL && log.detail() == LogDetail.ATE_ALL;
                    case ENERGY, SLEEP, VOMIT, SKIN, DRINKING, BREATHING, OTHER -> true;
                })
                .map(LogSnapshot::localDate)
                .collect(Collectors.toSet());
    }

    private static boolean isPoo(LogSnapshot log) {
        return log.detail() == LogDetail.POO || log.detail() == LogDetail.BOTH;
    }

    private static WatchEvent latest(List<WatchEvent> events) {
        return events.stream().max(Comparator.comparing(WatchEvent::on).thenComparingInt(WatchEvent::minute)).orElseThrow();
    }

    private static List<WatchEvent> sameKind(List<WatchEvent> events, WatchEvent like) {
        return events.stream()
                .filter(event -> event.sign() == like.sign() && event.kind() == like.kind() && event.detail() == like.detail())
                .toList();
    }
}
