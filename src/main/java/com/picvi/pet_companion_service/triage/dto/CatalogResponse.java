package com.picvi.pet_companion_service.triage.dto;

import com.picvi.pet_companion_service.triage.domain.CheckCatalog;

import java.util.List;

public record CatalogResponse(String version, List<String> categories, List<String> universalFlags,
                              List<CheckCatalog.Item> items, List<CheckCatalog.Question> questions,
                              List<String> substances) {
    public CatalogResponse {
        categories = List.copyOf(categories);
        universalFlags = List.copyOf(universalFlags);
        items = List.copyOf(items);
        questions = List.copyOf(questions);
        substances = List.copyOf(substances);
    }

    public static CatalogResponse of(CheckCatalog catalog) {
        return new CatalogResponse(catalog.version(), catalog.categories(), catalog.universalFlags(), catalog.items(),
                catalog.questions(), catalog.substances());
    }
}
