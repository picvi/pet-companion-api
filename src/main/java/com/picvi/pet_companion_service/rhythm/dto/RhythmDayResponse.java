package com.picvi.pet_companion_service.rhythm.dto;

import com.picvi.pet_companion_service.nutrition.api.MealPlan;
import com.picvi.pet_companion_service.dayevent.dto.DayEventDto;
import com.picvi.pet_companion_service.development.dto.ActivityRefDto;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.rhythm.domain.DayBlock;
import com.picvi.pet_companion_service.rhythm.domain.DayPlan;
import com.picvi.pet_companion_service.rhythm.domain.DaySettings;
import com.picvi.pet_companion_service.rhythm.domain.PlanFlag;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;
import com.picvi.pet_companion_service.rhythm.domain.RhythmStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record RhythmDayResponse(RhythmStatus status,
                                LocalDate date,
                                int ageWeeks,
                                RhythmRuleDto rule,
                                DaySettings settings,
                                DaySettings suggestedSettings,
                                List<DayBlock> blocks,
                                List<Integer> toiletTrips,
                                List<Integer> nightTrips,
                                Set<PlanFlag> flags,
                                List<PetLogDto> logs,
                                ActivityRefDto playProposal,
                                List<DayEventDto> events,
                                Integer bandNoteFromWeeks,
                                boolean checkinDue,
                                MealPlan mealPlan) {

    public static RhythmDayResponse notAvailable(LocalDate date, int ageWeeks) {
        return new RhythmDayResponse(RhythmStatus.NOT_AVAILABLE_FOR_AGE, date, ageWeeks, null, null, null,
                List.of(), List.of(), List.of(), Set.of(), List.of(), null, List.of(), null, false, null);
    }

    public static RhythmDayResponse setupNeeded(LocalDate date, int ageWeeks, RhythmRule rule, DaySettings suggested) {
        return new RhythmDayResponse(RhythmStatus.SETUP_NEEDED, date, ageWeeks, RhythmRuleDto.of(rule), null, suggested,
                List.of(), List.of(), List.of(), Set.of(), List.of(), null, List.of(), null, false, null);
    }

    public static RhythmDayResponse ready(LocalDate date, int ageWeeks, RhythmRule rule, DaySettings settings, DayPlan plan,
                                          List<PetLogDto> logs, ActivityRefDto playProposal, List<DayEventDto> events,
                                          Integer bandNoteFromWeeks, boolean checkinDue, MealPlan mealPlan) {
        return new RhythmDayResponse(RhythmStatus.READY, date, ageWeeks, RhythmRuleDto.of(rule), settings, null,
                plan.blocks(), plan.toiletTrips(), plan.nightTrips(), plan.flags(), logs, playProposal, events,
                bandNoteFromWeeks, checkinDue, mealPlan);
    }
}
