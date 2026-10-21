package com.picvi.pet_companion_service.petlog.dto;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogFood;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.NewLog;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreatePetLogRequest(@NotNull UUID clientId,
                                  @NotNull LogType type,
                                  LogDetail detail,
                                  Integer amountGrams,
                                  @NotNull LocalDate localDate,
                                  @NotNull @Min(0) @Max(1439) Integer minuteOfDay,
                                  LocalDate date,
                                  Boolean accident,
                                  Integer stoolScore,
                                  Set<LogFlag> flags,
                                  Integer weightGrams,
                                  Integer durationMinutes,
                                  Wellbeing wellbeing,
                                  String label,
                                  String dose,
                                  String note,
                                  List<LogFood> foods,
                                  Integer utcOffsetMinutes) {

    public NewLog toNewLog() {
        LogValues values = new LogValues(this.accident, this.stoolScore, this.flags, this.amountGrams, this.weightGrams,
                this.durationMinutes, this.wellbeing, this.label, this.dose, this.note, this.foods);
        return new NewLog(this.clientId, this.type, this.detail, values, this.localDate, this.minuteOfDay,
                this.utcOffsetMinutes);
    }
}
