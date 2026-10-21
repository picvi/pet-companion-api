package com.picvi.pet_companion_service.petlog.dto;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogEdit;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogFood;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record UpdatePetLogRequest(LogDetail detail,
                                  @NotNull LocalDate localDate,
                                  @NotNull @Min(0) @Max(1439) Integer minuteOfDay,
                                  Integer utcOffsetMinutes,
                                  Boolean accident,
                                  Integer stoolScore,
                                  Set<LogFlag> flags,
                                  Integer amountGrams,
                                  Integer weightGrams,
                                  Integer durationMinutes,
                                  Wellbeing wellbeing,
                                  String label,
                                  String dose,
                                  String note,
                                  List<LogFood> foods,
                                  @NotNull LocalDate date) {

    public LogEdit toEdit() {
        LogValues values = new LogValues(this.accident, this.stoolScore, this.flags, this.amountGrams, this.weightGrams,
                this.durationMinutes, this.wellbeing, this.label, this.dose, this.note, this.foods);
        return new LogEdit(this.detail, values, this.localDate, this.minuteOfDay, this.utcOffsetMinutes);
    }
}
