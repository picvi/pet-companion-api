package com.picvi.pet_companion_service.brief.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.picvi.pet_companion_service.brief.domain.Concern;
import com.picvi.pet_companion_service.brief.domain.Trend;
import com.picvi.pet_companion_service.brief.dto.BriefOptionsResponse.PeriodDto;
import com.picvi.pet_companion_service.care.dto.CareItemDto;
import com.picvi.pet_companion_service.condition.api.ConditionApi.ConditionSnapshot;
import com.picvi.pet_companion_service.medical.api.MedicalApi.VisitSnapshot;
import com.picvi.pet_companion_service.medical.domain.VisitReason;
import com.picvi.pet_companion_service.nutrition.dto.DietHistoryResponse;
import com.picvi.pet_companion_service.pet.entity.Sex;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VetBriefResponse(Instant generatedAt,
                               VisitReason reason,
                               PeriodDto period,
                               PetDto pet,
                               MainWorryDto mainWorry,
                               List<ConditionSnapshot> conditions,
                               FoodDto food,
                               List<DayDto> days,
                               CareDto care,
                               VisitSnapshot lastVisit,
                               List<QuestionDto> suggestedQuestions) {

    public record PetDto(String name, Sex sex, LocalDate birthDate, Integer ageWeeks, String breedCode, Boolean neutered,
                         String microchipNumber, WeightDto weight) {
    }

    public record WeightDto(int grams, LocalDate localDate, Integer previousGrams, LocalDate previousLocalDate) {
    }

    public record MainWorryDto(Concern concern, Trend trend, int count, LocalDate firstSeen, LocalDate lastSeen,
                               List<DailyCountDto> daily, List<LogFlag> flags, List<AlsoDto> also) {
    }

    public record DailyCountDto(LocalDate localDate, int count) {
    }

    public record AlsoDto(Concern concern, int count) {
    }

    public record FoodDto(int mealsLogged, DietHistoryResponse diet) {
    }

    public record DayDto(LocalDate localDate, int mealsLogged, List<EventDto> events) {
    }

    public record EventDto(UUID logId, LogType type, LogDetail detail, Integer stoolScore, List<LogFlag> flags, int minuteOfDay,
                           String note, Integer weightGrams, String label, String dose) {
    }

    public record CareDto(List<CareItemDto> vaccines, List<CareItemDto> parasites) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionDto(String code, Map<String, String> params) {
    }
}
