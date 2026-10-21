package com.picvi.pet_companion_service.nutrition.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pace rows + start date + slow-downs + pauses + the grams of one meal → the steps of a food change with their dates and
 * grams, and what goes in the bowl on a given day.
 *
 * <p>The plan counts <em>plan days</em>. A day advances the plan unless it is a paused day, so a pause repeats the step
 * it began on. A slow-down adds days to one step. Dates of days that have not happened yet assume the plan runs as of the
 * reference day.
 *
 * <p>Rounding: the smaller share is rounded up (half up), the larger one is what is left. 30 g at 25% new is 8 g new and
 * 22 g old; at 75% new it is 8 g old and 22 g new. A trace of the old food at the end, and of the new one at the start, is never rounded away.
 */
public class TransitionPlanCalculator {

    public record PaceStep(int order, int days, int newPercent) {
    }

    /** The days a pause held the plan back: from pausedOn up to, not including, resumedOn (null = still paused). */
    public record PauseInterval(LocalDate pausedOn, LocalDate resumedOn) {
    }

    public record PlanInput(List<PaceStep> pace,
                            LocalDate startOn,
                            List<PauseInterval> pauses,
                            Map<Integer, Integer> extensions,
                            Integer perMealGrams) {
    }

    public record PlanStep(int order, LocalDate from, LocalDate to, int newPercent, Integer oldGrams, Integer newGrams, int extendedDays) {
    }

    /** What is in the bowl on one day of the plan. */
    public record DayMix(PlanStep step, int dayOfPlan, int totalDays) {
    }

    public record Plan(List<PlanStep> steps, int totalDays, LocalDate lastDay, PlanInput input) {
        private PlanStep stepOfPlanDay(int planDay) {
            int first = 0;
            for (PlanStep step : this.steps) {
                int length = this.lengthOf(step);
                if (planDay < first + length) {
                    return step;
                }
                first += length;
            }
            return null;
        }

        private int lengthOf(PlanStep step) {
            return this.input.pace().stream().filter(p -> p.order() == step.order()).mapToInt(PaceStep::days).sum() + step.extendedDays();
        }

        /** The mix on a date, or empty before the plan starts or after its last day. */
        public Optional<DayMix> mixOn(LocalDate date) {
            if (date.isBefore(this.input.startOn())) {
                return Optional.empty();
            }
            int planDay = planDayOn(this.input, date);
            PlanStep step = planDay >= this.totalDays ? null : this.stepOfPlanDay(planDay);
            return step == null ? Optional.empty() : Optional.of(new DayMix(step, planDay + 1, this.totalDays));
        }

        public boolean isOver(LocalDate date) {
            return !date.isBefore(this.input.startOn()) && planDayOn(this.input, date) >= this.totalDays;
        }
    }

    /** @param asOf the day the plan is viewed on; later steps get dates counted from it */
    public Plan plan(PlanInput input, LocalDate asOf) {
        List<PlanStep> steps = new ArrayList<>();
        int firstPlanDay = 0;
        for (PaceStep pace : input.pace().stream().sorted(java.util.Comparator.comparingInt(PaceStep::order)).toList()) {
            int extra = input.extensions().getOrDefault(pace.order(), 0);
            int length = pace.days() + extra;
            int lastPlanDay = firstPlanDay + length - 1;
            int[] grams = this.split(input.perMealGrams(), pace.newPercent());
            steps.add(new PlanStep(pace.order(), this.firstDate(input, firstPlanDay, asOf), this.lastDate(input, lastPlanDay, asOf),
                    pace.newPercent(), grams == null ? null : grams[0], grams == null ? null : grams[1], extra));
            firstPlanDay += length;
        }
        LocalDate lastDay = steps.isEmpty() ? input.startOn() : steps.get(steps.size() - 1).to();
        return new Plan(List.copyOf(steps), firstPlanDay, lastDay, input);
    }

    /** {old, new} grams of one meal, or null when there is no amount. */
    int[] split(Integer perMealGrams, int newPercent) {
        if (perMealGrams == null) {
            return null;
        }
        double newShare = perMealGrams * newPercent / 100.0;
        if (newPercent <= 50) {
            int newGrams = (int) Math.floor(newShare + 0.5);
            return new int[]{perMealGrams - newGrams, newGrams};
        }
        int oldGrams = (int) Math.floor(perMealGrams - newShare + 0.5);
        return new int[]{oldGrams, perMealGrams - oldGrams};
    }

    /** Plan days that have gone by on a date: every day since the start except paused ones. */
    static int planDayOn(PlanInput input, LocalDate date) {
        int days = (int) ChronoUnit.DAYS.between(input.startOn(), date);
        int paused = 0;
        for (PauseInterval pause : input.pauses()) {
            LocalDate end = pause.resumedOn() == null || pause.resumedOn().isAfter(date) ? date : pause.resumedOn();
            if (end.isAfter(pause.pausedOn())) {
                paused += (int) ChronoUnit.DAYS.between(pause.pausedOn(), end);
            }
        }
        return Math.max(0, days - paused);
    }

    private LocalDate firstDate(PlanInput input, int planDay, LocalDate asOf) {
        for (LocalDate d = input.startOn(); !d.isAfter(asOf); d = d.plusDays(1)) {
            if (planDayOn(input, d) == planDay) {
                return d;
            }
        }
        LocalDate base = asOf.isBefore(input.startOn()) ? input.startOn() : asOf;
        return base.plusDays(planDay - planDayOn(input, base));
    }

    private LocalDate lastDate(PlanInput input, int planDay, LocalDate asOf) {
        LocalDate found = null;
        for (LocalDate d = input.startOn(); !d.isAfter(asOf); d = d.plusDays(1)) {
            if (planDayOn(input, d) == planDay) {
                found = d;
            }
        }
        return found != null ? found : this.firstDate(input, planDay, asOf);
    }
}
