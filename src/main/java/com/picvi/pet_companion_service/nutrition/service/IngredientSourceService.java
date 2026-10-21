package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.dto.IngredientSourceDto;
import com.picvi.pet_companion_service.nutrition.dto.IngredientSourcesResponse;
import com.picvi.pet_companion_service.nutrition.repository.IngredientSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IngredientSourceService {
    private final IngredientSourceRepository repository;

    @Transactional(readOnly = true)
    public IngredientSourcesResponse list() {
        return new IngredientSourcesResponse(this.repository.findAllByOrderBySortOrderAsc().stream()
                .filter(source -> "ACTIVE".equals(source.getStatus()))
                .map(IngredientSourceDto::of)
                .toList());
    }
}
