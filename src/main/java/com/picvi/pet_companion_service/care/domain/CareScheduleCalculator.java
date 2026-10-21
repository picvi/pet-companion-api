package com.picvi.pet_companion_service.care.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class CareScheduleCalculator {
    private static final int SOON_DAYS = 7;
    private static final Comparator<CareItemSchedule> URGENCY = Comparator.comparing(CareItemSchedule::status);

    public CareItemSchedule calculate(CareItemDefinition item,
                                      LocalDate birthDate,
                                      List<LocalDate> eventDates,
                                      CareItemSettings settings,
                                      LocalDate today) {
        List<LocalDate> doses = List.copyOf(new TreeSet<>(eventDates));
        LocalDate lastDose = doses.isEmpty() ? null : doses.get(doses.size() - 1);

        CareStepDefinition step = this.nextStep(item, doses.size(), birthDate, lastDose);
        CarePhase phase = step == null ? null : CarePhase.valueOf(step.kind().name());
        boolean inSeries = step != null && step.kind() == StepKind.SERIES;

        LocalDate availableFrom = null;
        LocalDate legalDeadline = null;
        LocalDate dueOn = null;
        if (step != null) {
            if (item.hasLegalDeadline() && lastDose == null) {
                availableFrom = birthDate.plusMonths(item.legalMinAgeMonths());
                legalDeadline = availableFrom.plusDays(item.legalWindowDays());
                dueOn = legalDeadline;
            } else {
                dueOn = this.dueOn(item, step, birthDate, lastDose, settings);
            }
            if (step.hasSeason()) {
                dueOn = this.inSeason(dueOn, step);
            }
        }

        boolean outOfSeason = step != null && step.hasSeason() && !this.isInSeason(today, step);
        boolean paused = outOfSeason || (settings.pausedUntil() != null && today.isBefore(settings.pausedUntil()));
        CareStatus status = paused
                ? CareStatus.PAUSED
                : this.status(doses.size(), availableFrom, dueOn, legalDeadline, today);

        Integer defaultInterval = item.intervalEditable()
                ? item.openEndedRepeat().map(CareStepDefinition::gapDays).orElse(null)
                : null;
        Integer customInterval = item.intervalEditable() ? settings.intervalDays() : null;

        return new CareItemSchedule(
                item.code(),
                item.category(),
                item.requirement(),
                status,
                phase,
                inSeries ? doses.size() + 1 : null,
                inSeries ? item.seriesTotal() : null,
                availableFrom,
                dueOn,
                legalDeadline,
                lastDose,
                this.remindOn(settings, paused, availableFrom, legalDeadline, dueOn, today),
                settings.remindersEnabled(),
                settings.pausedUntil(),
                item.intervalEditable(),
                defaultInterval,
                customInterval
        );
    }

    public List<CareItemSchedule> order(List<CareItemSchedule> schedules) {
        return schedules.stream()
                .sorted(Comparator.comparing(CareItemSchedule::requirement).thenComparing(URGENCY))
                .toList();
    }

    public Optional<String> alertCode(List<CareItemSchedule> schedules) {
        return schedules.stream()
                .filter(CareItemSchedule::remindersEnabled)
                .filter(schedule -> schedule.status() == CareStatus.OVERDUE
                        || schedule.status() == CareStatus.DUE
                        || schedule.status() == CareStatus.DUE_SOON)
                .min(Comparator.comparing(CareItemSchedule::status).thenComparing(CareItemSchedule::requirement))
                .map(schedule -> schedule.code() + "_" + schedule.status().name());
    }

    private CareStepDefinition nextStep(CareItemDefinition item, int doseCount, LocalDate birthDate, LocalDate lastDose) {
        List<CareStepDefinition> doseSteps = item.doseSteps();
        if (doseCount < doseSteps.size()) {
            return doseSteps.get(doseCount);
        }
        List<CareStepDefinition> repeatSteps = item.repeatSteps();
        if (lastDose == null) {
            return repeatSteps.isEmpty() ? null : repeatSteps.get(0);
        }
        long ageWeeksAtLastDose = ChronoUnit.DAYS.between(birthDate, lastDose) / 7;
        return repeatSteps.stream()
                .filter(step -> step.maxAgeWeeks() == null || ageWeeksAtLastDose < step.maxAgeWeeks())
                .findFirst()
                .orElse(null);
    }

    private LocalDate dueOn(CareItemDefinition item,
                            CareStepDefinition step,
                            LocalDate birthDate,
                            LocalDate lastDose,
                            CareItemSettings settings) {
        LocalDate byAge = step.minAgeWeeks() == null ? null : birthDate.plusWeeks(step.minAgeWeeks());
        LocalDate byGap = lastDose == null ? null : this.afterLastDose(item, step, lastDose, settings);

        if (byAge == null && byGap == null) {
            return birthDate;
        }
        if (byAge == null) {
            return byGap;
        }
        if (byGap == null) {
            return byAge;
        }
        return byAge.isAfter(byGap) ? byAge : byGap;
    }

    private LocalDate afterLastDose(CareItemDefinition item,
                                    CareStepDefinition step,
                                    LocalDate lastDose,
                                    CareItemSettings settings) {
        if (item.hasLegalDeadline() && step.kind() == StepKind.REPEAT && item.legalRepeatMonths() != null) {
            return lastDose.plusMonths(item.legalRepeatMonths());
        }
        if (step.gapDays() == null) {
            return null;
        }
        boolean customApplies = item.intervalEditable() && step.isOpenEndedRepeat() && settings.intervalDays() != null;
        return lastDose.plusDays(customApplies ? settings.intervalDays() : step.gapDays());
    }

    private CareStatus status(int doseCount,
                              LocalDate availableFrom,
                              LocalDate dueOn,
                              LocalDate legalDeadline,
                              LocalDate today) {
        if (dueOn == null) {
            return CareStatus.UP_TO_DATE;
        }
        if (legalDeadline != null) {
            return this.legalStatus(availableFrom, legalDeadline, today);
        }
        if (doseCount == 0) {
            return this.neverLoggedStatus(dueOn, today);
        }
        if (today.isAfter(dueOn)) {
            return CareStatus.OVERDUE;
        }
        if (today.isEqual(dueOn)) {
            return CareStatus.DUE;
        }
        return this.isSoon(dueOn, today) ? CareStatus.DUE_SOON : CareStatus.UP_TO_DATE;
    }

    private CareStatus legalStatus(LocalDate availableFrom, LocalDate legalDeadline, LocalDate today) {
        if (today.isAfter(legalDeadline)) {
            return CareStatus.OVERDUE;
        }
        if (!today.isBefore(availableFrom)) {
            return CareStatus.DUE;
        }
        return this.isSoon(availableFrom, today) ? CareStatus.DUE_SOON : CareStatus.NOT_YET_DUE;
    }

    private CareStatus neverLoggedStatus(LocalDate dueOn, LocalDate today) {
        if (!today.isBefore(dueOn)) {
            return CareStatus.DUE;
        }
        return this.isSoon(dueOn, today) ? CareStatus.DUE_SOON : CareStatus.NOT_YET_DUE;
    }

    private boolean isSoon(LocalDate date, LocalDate today) {
        return ChronoUnit.DAYS.between(today, date) <= SOON_DAYS;
    }

    private List<LocalDate> remindOn(CareItemSettings settings,
                                     boolean paused,
                                     LocalDate availableFrom,
                                     LocalDate legalDeadline,
                                     LocalDate dueOn,
                                     LocalDate today) {
        if (!settings.remindersEnabled() || paused || dueOn == null) {
            return List.of();
        }
        TreeSet<LocalDate> dates = new TreeSet<>();
        if (availableFrom != null) {
            dates.add(availableFrom);
        }
        dates.add(dueOn.minusDays(SOON_DAYS));
        dates.add(dueOn);
        return dates.stream()
                .filter(date -> !date.isBefore(today))
                .filter(date -> settings.pausedUntil() == null || !date.isBefore(settings.pausedUntil()))
                .toList();
    }

    private boolean isInSeason(LocalDate date, CareStepDefinition step) {
        int month = date.getMonthValue();
        return month >= step.seasonStartMonth() && month <= step.seasonEndMonth();
    }

    private LocalDate inSeason(LocalDate date, CareStepDefinition step) {
        if (this.isInSeason(date, step)) {
            return date;
        }
        int year = date.getMonthValue() > step.seasonEndMonth() ? date.getYear() + 1 : date.getYear();
        return LocalDate.of(year, step.seasonStartMonth(), 1);
    }
}
