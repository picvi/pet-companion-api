package com.picvi.pet_companion_service.brief.service;

import com.picvi.pet_companion_service.brief.domain.BriefLimits;
import com.picvi.pet_companion_service.brief.domain.BriefReason;
import com.picvi.pet_companion_service.brief.domain.Concern;
import com.picvi.pet_companion_service.brief.domain.ConcernCounter;
import com.picvi.pet_companion_service.brief.domain.ConcernCounter.Tally;
import com.picvi.pet_companion_service.brief.domain.QuestionSuggester;
import com.picvi.pet_companion_service.brief.domain.QuestionSuggester.VaccineDue;
import com.picvi.pet_companion_service.brief.domain.TrendCalculator;
import com.picvi.pet_companion_service.brief.dto.BriefOptionsResponse;
import com.picvi.pet_companion_service.brief.dto.BriefOptionsResponse.ConcernOptionDto;
import com.picvi.pet_companion_service.brief.dto.BriefOptionsResponse.PeriodDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.AlsoDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.CareDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.DailyCountDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.DayDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.EventDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.FoodDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.MainWorryDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.PetDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.QuestionDto;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse.WeightDto;
import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.care.domain.CareCategory;
import com.picvi.pet_companion_service.care.domain.CareStatus;
import com.picvi.pet_companion_service.care.dto.CareItemDto;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.exception.brief.InvalidBriefRequest;
import com.picvi.pet_companion_service.medical.api.MedicalApi;
import com.picvi.pet_companion_service.medication.api.MedicationApi;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.nutrition.dto.DietHistoryResponse;
import com.picvi.pet_companion_service.pet.dto.PetResponse;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.WeightPoint;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VetBriefService {
    private static final Set<LogFlag> WORRY_FLAGS = EnumSet.of(LogFlag.BLOOD, LogFlag.MUCUS, LogFlag.WORMS, LogFlag.BLACK, LogFlag.STRAINING);

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final PetLogApi petLogApi;
    private final ConditionApi conditionApi;
    private final CareApi careApi;
    private final NutritionApi nutritionApi;
    private final MedicalApi medicalApi;
    private final MedicationApi medicationApi;
    private final ConcernCounter concernCounter;
    private final TrendCalculator trendCalculator;
    private final QuestionSuggester questionSuggester;
    private final Clock clock;

    public BriefOptionsResponse options(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetResponse pet = this.petService.getPetById(ownerId, petId);
        LocalDate lookbackFrom = date.minusDays(BriefLimits.CONCERN_LOOKBACK_DAYS - 1L);
        Map<Concern, Tally> tallies = this.concernCounter.count(this.petLogApi.logsBetween(petId, lookbackFrom, date));
        List<ConcernOptionDto> concerns = tallies.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Concern, Tally>>comparingInt(entry -> entry.getValue().count()).reversed()
                        .thenComparing(entry -> entry.getValue().lastSeen(), Comparator.reverseOrder()))
                .map(entry -> new ConcernOptionDto(entry.getKey(), entry.getValue().count(), entry.getValue().firstSeen(),
                        entry.getValue().lastSeen(), defaultFrom(entry.getValue(), pet.birthDate())))
                .toList();
        return new BriefOptionsResponse(date, defaultPeriod(date), concerns);
    }

    public VetBriefResponse brief(UUID ownerId, UUID petId, LocalDate date, String reasonCode, String concernCode, LocalDate from,
                                  LocalDate to) {
        this.clientDateValidator.validate(date);
        BriefReason reason = parseReason(reasonCode);
        Concern concern = parseConcern(concernCode);
        PetResponse pet = this.petService.getPetById(ownerId, petId);
        PeriodDto period = this.resolvePeriod(petId, pet, date, concern, from, to);

        List<LogSnapshot> logs = this.petLogApi.logsBetween(petId, period.from(), period.to());
        Map<Concern, Tally> tallies = this.concernCounter.count(logs);
        DietHistoryResponse diet = this.nutritionApi.dietHistory(petId, period.from(), period.to());
        List<CareItemDto> careItems = this.careApi.schedule(ownerId, petId, date).items();
        List<CareItemDto> vaccines = careItems.stream().filter(item -> item.category() == CareCategory.VACCINE).toList();
        List<CareItemDto> parasites = careItems.stream().filter(item -> item.category() == CareCategory.PARASITE).toList();
        MainWorryDto mainWorry = this.mainWorry(concern, tallies, logs, period);

        return new VetBriefResponse(Instant.now(this.clock), reason, period, this.petDto(petId, pet, date), mainWorry,
                this.conditionApi.openConditions(petId, date),
                new FoodDto((int) logs.stream().filter(log -> log.type() == LogType.MEAL).count(), diet),
                days(logs), new CareDto(vaccines, parasites), this.medicationApi.coursesBetween(petId, period.from(), period.to(), date),
                this.medicalApi.lastVisit(petId, date).orElse(null),
                this.questions(concern, diet, vaccines, date));
    }

    private PeriodDto resolvePeriod(UUID petId, PetResponse pet, LocalDate date, Concern concern, LocalDate from, LocalDate to) {
        LocalDate periodTo = to == null ? date : to;
        LocalDate periodFrom = from;
        if (periodFrom == null) {
            periodFrom = this.concernDefaultFrom(petId, pet, date, concern).orElse(defaultPeriod(date).from());
            if (periodFrom.isAfter(periodTo)) {
                periodFrom = periodTo;
            }
        }
        if (periodFrom.isAfter(periodTo)) {
            throw new InvalidBriefRequest("from", InvalidBriefRequest.INVALID);
        }
        if (ChronoUnit.DAYS.between(periodFrom, periodTo) + 1 > BriefLimits.MAX_PERIOD_DAYS) {
            throw new InvalidBriefRequest("from", InvalidBriefRequest.OUT_OF_RANGE);
        }
        return new PeriodDto(periodFrom, periodTo);
    }

    private Optional<LocalDate> concernDefaultFrom(UUID petId, PetResponse pet, LocalDate date, Concern concern) {
        if (concern == null) {
            return Optional.empty();
        }
        LocalDate lookbackFrom = date.minusDays(BriefLimits.CONCERN_LOOKBACK_DAYS - 1L);
        Tally tally = this.concernCounter.count(this.petLogApi.logsBetween(petId, lookbackFrom, date)).get(concern);
        return Optional.ofNullable(tally).map(found -> defaultFrom(found, pet.birthDate()));
    }

    private MainWorryDto mainWorry(Concern concern, Map<Concern, Tally> tallies, List<LogSnapshot> logs, PeriodDto period) {
        Tally tally = concern == null ? null : tallies.get(concern);
        if (tally == null) {
            return null;
        }
        List<DailyCountDto> daily = new ArrayList<>();
        for (LocalDate day = period.from(); !day.isAfter(period.to()); day = day.plusDays(1)) {
            daily.add(new DailyCountDto(day, tally.daily().getOrDefault(day, 0)));
        }
        List<LogFlag> flags = logs.stream()
                .filter(concern::matches)
                .flatMap(log -> log.flags().stream())
                .filter(WORRY_FLAGS::contains)
                .distinct()
                .sorted()
                .toList();
        List<AlsoDto> also = tallies.entrySet().stream()
                .filter(entry -> entry.getKey() != concern)
                .sorted(Comparator.<Map.Entry<Concern, Tally>>comparingInt(entry -> entry.getValue().count()).reversed())
                .map(entry -> new AlsoDto(entry.getKey(), entry.getValue().count()))
                .toList();
        return new MainWorryDto(concern, this.trendCalculator.calculate(tally.daily(), period.to()), tally.count(), tally.firstSeen(),
                tally.lastSeen(), daily, flags, also);
    }

    private PetDto petDto(UUID petId, PetResponse pet, LocalDate date) {
        Integer ageWeeks = pet.birthDate() == null ? null : (int) (ChronoUnit.DAYS.between(pet.birthDate(), date) / 7);
        return new PetDto(pet.name(), pet.sex(), pet.birthDate(), ageWeeks, pet.breedCode(), pet.neutered(), pet.microchipNumber(),
                this.weight(petId, date));
    }

    private WeightDto weight(UUID petId, LocalDate date) {
        Optional<WeightPoint> latest = this.petLogApi.lastWeightBefore(petId, date);
        if (latest.isEmpty()) {
            return null;
        }
        Optional<WeightPoint> previous = this.petLogApi.lastWeightBefore(petId, latest.get().localDate().minusDays(1));
        return new WeightDto(latest.get().grams(), latest.get().localDate(), previous.map(WeightPoint::grams).orElse(null),
                previous.map(WeightPoint::localDate).orElse(null));
    }

    private static List<DayDto> days(List<LogSnapshot> logs) {
        Map<LocalDate, List<LogSnapshot>> byDay = new TreeMap<>(Comparator.reverseOrder());
        logs.forEach(log -> byDay.computeIfAbsent(log.localDate(), day -> new ArrayList<>()).add(log));
        return byDay.entrySet().stream()
                .map(entry -> new DayDto(entry.getKey(),
                        (int) entry.getValue().stream().filter(log -> log.type() == LogType.MEAL).count(),
                        entry.getValue().stream()
                                .filter(log -> log.type() != LogType.MEAL || log.detail() != LogDetail.ATE_ALL)
                                .sorted(Comparator.comparingInt(LogSnapshot::minuteOfDay))
                                .map(VetBriefService::event)
                                .toList()))
                .toList();
    }

    private static EventDto event(LogSnapshot log) {
        return new EventDto(log.logId(), log.type(), log.detail(), log.stoolScore(), log.flags().stream().sorted().toList(),
                log.minuteOfDay(), log.note(), log.weightGrams(), log.label(), log.dose());
    }

    private List<QuestionDto> questions(Concern concern, DietHistoryResponse diet, List<CareItemDto> vaccines, LocalDate date) {
        List<LocalDate> foodChangeDates = diet.changes().stream().map(change -> change.from()).toList();
        List<VaccineDue> due = vaccines.stream()
                .filter(item -> item.status() != CareStatus.PAUSED)
                .map(item -> new VaccineDue(item.code(), item.dueOn()))
                .toList();
        return this.questionSuggester.suggest(concern, foodChangeDates, due, date).stream()
                .map(question -> new QuestionDto(question.code(), question.params()))
                .toList();
    }

    private static PeriodDto defaultPeriod(LocalDate date) {
        return new PeriodDto(date.minusDays(BriefLimits.DEFAULT_PERIOD_DAYS - 1L), date);
    }

    private static LocalDate defaultFrom(Tally tally, LocalDate birthDate) {
        LocalDate from = tally.firstSeen().minusDays(BriefLimits.LEAD_IN_DAYS);
        return birthDate != null && from.isBefore(birthDate) ? birthDate : from;
    }

    private static BriefReason parseReason(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidBriefRequest("reason", InvalidBriefRequest.REQUIRED);
        }
        try {
            return BriefReason.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidBriefRequest("reason", InvalidBriefRequest.INVALID);
        }
    }

    private static Concern parseConcern(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return Concern.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidBriefRequest("concern", InvalidBriefRequest.INVALID);
        }
    }
}
