package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.SafetyLevel;
import com.picvi.pet_companion_service.nutrition.entity.FoodSafetyItem;

import java.util.List;

public record FoodSafetyItemDto(String code,
                                SafetyLevel level,
                                Integer quickOrder,
                                Localized name,
                                Localized why,
                                Localized how,
                                Aliases aliases,
                                List<String> sources) {

    public static FoodSafetyItemDto of(FoodSafetyItem item) {
        Localized how = item.getHowEn() == null ? null : new Localized(item.getHowEn(), item.getHowPl());
        return new FoodSafetyItemDto(item.getCode(), item.getLevel(), item.getQuickOrder(),
                new Localized(item.getNameEn(), item.getNamePl()),
                new Localized(item.getWhyEn(), item.getWhyPl()),
                how,
                new Aliases(item.getAliasesEn(), item.getAliasesPl()),
                item.getSources());
    }
}
