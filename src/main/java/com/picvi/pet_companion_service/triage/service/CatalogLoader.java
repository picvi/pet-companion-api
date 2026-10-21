package com.picvi.pet_companion_service.triage.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.picvi.pet_companion_service.triage.domain.CheckCatalog;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CatalogLoader {
    private static final TypeReference<CheckCatalog> CATALOG_TYPE = new TypeReference<>() { };
    private final ResourceLoader resourceLoader;
    private final YAMLMapper yamlMapper = new YAMLMapper();
    @Getter
    private CheckCatalog catalog;

    public CatalogLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void load() {
        try (var input = this.resourceLoader.getResource("classpath:triage/catalog.yml").getInputStream()) {
            this.catalog = this.yamlMapper.readValue(input, CATALOG_TYPE);
            this.validate(this.catalog);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load triage catalogue", exception);
        }
    }

    private void validate(CheckCatalog catalog) {
        if (catalog.version() == null || catalog.version().isBlank()) {
            throw new IllegalStateException("Triage catalogue version is required");
        }
        this.unique(catalog.categories(), "category");
        this.unique(catalog.universalFlags(), "universal flag");
        this.unique(catalog.substances(), "substance");
        this.unique(catalog.items().stream().map(CheckCatalog.Item::code).toList(), "item");
        this.unique(catalog.questions().stream().map(CheckCatalog.Question::code).toList(), "question");
        Set<String> itemFlags = new HashSet<>(catalog.universalFlags());
        catalog.items().forEach(item -> itemFlags.addAll(item.flags()));
        for (CheckCatalog.Item item : catalog.items()) {
            if (!catalog.categories().contains(item.category())) {
                throw new IllegalStateException("Unknown category " + item.category() + " on item " + item.code());
            }
            if (!itemFlags.containsAll(item.flags())) {
                throw new IllegalStateException("Unknown flag on item " + item.code());
            }
            for (String questionCode : item.questions()) {
                CheckCatalog.Question question = catalog.question(questionCode);
                if (question == null || question.options().isEmpty()) {
                    throw new IllegalStateException("Missing question or options " + questionCode + " on " + item.code());
                }
                this.unique(question.options(), "answer option in " + questionCode);
            }
        }
    }

    private void unique(List<String> values, String label) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new IllegalStateException("Duplicate " + label + " code");
        }
    }
}
