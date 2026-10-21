package com.picvi.pet_companion_service.health.dto;

import com.picvi.pet_companion_service.health.domain.BodySign;
import com.picvi.pet_companion_service.health.domain.CardState;
import com.picvi.pet_companion_service.health.domain.HealthSummary;
import com.picvi.pet_companion_service.health.domain.NoteCode;
import com.picvi.pet_companion_service.health.domain.SignState;
import com.picvi.pet_companion_service.health.domain.WatchKind;
import com.picvi.pet_companion_service.nutrition.api.FoodChangeNow;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.WeightPoint;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;

import java.time.LocalDate;
import java.util.List;

public record HealthSummaryResponse(LocalDate date,
                                    CardState state,
                                    HeadlineDto headline,
                                    NoteDto note,
                                    List<SignDto> signs,
                                    FoodChangeDto foodChange,
                                    WeightDto weight,
                                    LastMedicineDto lastMedicine,
                                    List<PetLogDto> diary) {

    public record HeadlineDto(BodySign sign, WatchKind kind, LogDetail detail, int count, LocalDate from, LocalDate lastOn) {
        static HeadlineDto of(HealthSummary.Headline headline) {
            return headline == null ? null
                    : new HeadlineDto(headline.sign(), headline.kind(), headline.detail(), headline.count(), headline.from(), headline.lastOn());
        }
    }

    public record NoteDto(NoteCode code, BodySign sign) {
        static NoteDto of(HealthSummary.Note note) {
            return new NoteDto(note.code(), note.sign());
        }
    }

    public record SignDto(BodySign sign, SignState state, WatchDto watch) {
        static SignDto of(HealthSummary.SignSummary summary) {
            return new SignDto(summary.sign(), summary.state(), WatchDto.of(summary.watch()));
        }
    }

    public record WatchDto(WatchKind kind, LogDetail detail, int count, LocalDate from, LocalDate to, LocalDate lastOn,
                           int lastMinute, LocalDate normalSince) {
        static WatchDto of(HealthSummary.SignWatch watch) {
            return watch == null ? null
                    : new WatchDto(watch.kind(), watch.detail(), watch.count(), watch.from(), watch.to(), watch.lastOn(),
                    watch.lastMinute(), watch.normalSince());
        }
    }

    public record FoodChangeDto(FoodChangeNow.State state, LocalDate startedOn) {
        static FoodChangeDto of(FoodChangeNow change) {
            return change == null ? null : new FoodChangeDto(change.state(), change.startedOn());
        }
    }

    public record WeightDto(int grams, LocalDate on, Integer previousGrams, LocalDate previousOn) {
        static WeightDto of(List<WeightPoint> lastTwo) {
            if (lastTwo.isEmpty()) {
                return null;
            }
            WeightPoint latest = lastTwo.get(0);
            WeightPoint previous = lastTwo.size() > 1 ? lastTwo.get(1) : null;
            return new WeightDto(latest.grams(), latest.localDate(),
                    previous == null ? null : previous.grams(), previous == null ? null : previous.localDate());
        }
    }

    public record LastMedicineDto(String label, String dose, LocalDate on) {
        static LastMedicineDto of(LogSnapshot medicine) {
            return medicine == null ? null : new LastMedicineDto(medicine.label(), medicine.dose(), medicine.localDate());
        }
    }

    public static HealthSummaryResponse of(LocalDate date, HealthSummary summary, List<WeightPoint> lastTwoWeights,
                                           LogSnapshot lastMedicine, List<PetLogDto> diary) {
        return new HealthSummaryResponse(date, summary.state(), HeadlineDto.of(summary.headline()), NoteDto.of(summary.note()),
                summary.signs().stream().map(SignDto::of).toList(), FoodChangeDto.of(summary.foodChange()),
                WeightDto.of(lastTwoWeights), LastMedicineDto.of(lastMedicine), diary);
    }
}
