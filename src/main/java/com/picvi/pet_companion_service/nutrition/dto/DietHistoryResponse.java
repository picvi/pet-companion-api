package com.picvi.pet_companion_service.nutrition.dto;

import java.time.LocalDate;
import java.util.List;

/** What the dog ate between two days, for the vet brief. */
public record DietHistoryResponse(LocalDate from,
                                  LocalDate to,
                                  List<HistoryFoodDto> mainFoods,
                                  List<HistoryFoodDto> treatsAndChews,
                                  List<HistoryChangeDto> changes,
                                  List<ProteinExposureDto> proteins) {
}
