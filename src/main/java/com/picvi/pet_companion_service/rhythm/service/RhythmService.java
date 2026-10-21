package com.picvi.pet_companion_service.rhythm.service;

import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrence;
import com.picvi.pet_companion_service.dayevent.dto.DayEventDto;
import com.picvi.pet_companion_service.dayevent.service.DayEventService;
import com.picvi.pet_companion_service.development.dto.ActivityRefDto;
import com.picvi.pet_companion_service.development.service.TodayService;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import com.picvi.pet_companion_service.rhythm.domain.DayPlan;
import com.picvi.pet_companion_service.rhythm.domain.DayPlanInput;
import com.picvi.pet_companion_service.rhythm.domain.DayPlanner;
import com.picvi.pet_companion_service.rhythm.domain.DaySettings;
import com.picvi.pet_companion_service.rhythm.domain.DefaultDayProposer;
import com.picvi.pet_companion_service.rhythm.domain.FixedEvent;
import com.picvi.pet_companion_service.rhythm.domain.LoggedEvent;
import com.picvi.pet_companion_service.rhythm.domain.RhythmNotices;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRuleResolver;
import com.picvi.pet_companion_service.rhythm.dto.RhythmDayResponse;
import com.picvi.pet_companion_service.rhythm.entity.PetRhythmSettings;
import com.picvi.pet_companion_service.rhythm.repository.PetRhythmSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RhythmService {
    private static final Set<LogType> RHYTHM_TYPES = EnumSet.of(LogType.TOILET, LogType.MEAL, LogType.SLEEP, LogType.WAKE);

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final RhythmRuleLoader ruleLoader;
    private final RhythmRuleResolver ruleResolver;
    private final PetRhythmSettingsRepository settingsRepository;
    private final DefaultDayProposer defaultDayProposer;
    private final DayPlanner dayPlanner;
    private final PetLogService logService;
    private final TodayService todayService;
    private final PlayProposer playProposer;
    private final DayEventService eventService;
    private final RhythmNotices notices;
    private final NutritionApi nutritionApi;

    public RhythmDayResponse getDay(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        return this.buildDay(profile, date);
    }

    public RhythmDayResponse buildDay(PetProfile profile, LocalDate date) {
        if (date.isBefore(profile.birthDate())) {
            throw new InvalidDate();
        }

        int ageWeeks = (int) ChronoUnit.WEEKS.between(profile.birthDate(), date);
        Optional<RhythmRule> rule = this.ruleResolver.resolve(this.ruleLoader.loadAll(), ageWeeks, profile.sizeCategory());
        if (rule.isEmpty()) {
            return RhythmDayResponse.notAvailable(date, ageWeeks);
        }

        return this.settingsRepository.findByPetId(profile.id())
                .map(settings -> this.readyDay(profile, date, ageWeeks, rule.get(), settings))
                .orElseGet(() -> RhythmDayResponse.setupNeeded(date, ageWeeks, rule.get(), this.defaultDayProposer.propose(rule.get())));
    }

    private RhythmDayResponse readyDay(PetProfile profile, LocalDate date, int ageWeeks, RhythmRule rule, PetRhythmSettings saved) {
        DaySettings settings = saved.toDaySettings();
        List<PetLogDto> logs = this.logService.logsOn(profile.id(), date).stream()
                .filter(log -> RHYTHM_TYPES.contains(log.type()))
                .toList();
        List<LoggedEvent> events = logs.stream().map(log -> new LoggedEvent(log.type(), log.minuteOfDay())).toList();
        List<EventOccurrence> occurrences = this.eventService.occurrencesOn(profile.id(), date);
        List<FixedEvent> fixedEvents = occurrences.stream()
                .filter(EventOccurrence::planAround)
                .map(occurrence -> new FixedEvent(occurrence.startMinute(), occurrence.endMinute()))
                .toList();
        DayPlan plan = this.dayPlanner.plan(new DayPlanInput(rule, settings, events, fixedEvents));
        ActivityRefDto playProposal = this.playProposer.propose(this.todayService.buildToday(profile, date)).orElse(null);
        List<DayEventDto> eventDtos = occurrences.stream().map(occurrence -> DayEventDto.of(occurrence, date)).toList();
        return RhythmDayResponse.ready(date, ageWeeks, rule, settings, plan, logs, playProposal, eventDtos,
                this.notices.bandNoteFromWeeks(saved.getBandNoteSeenFromWeeks(), rule),
                this.notices.checkinDue(saved.getCreatedOn(), saved.getCheckinAnsweredOn(), date),
                this.nutritionApi.portionsFor(profile.id(), date, settings.mealMinutes().size()));
    }
}
