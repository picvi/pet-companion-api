package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.ReferenceCodeChecker;
import com.picvi.pet_companion_service.nutrition.repository.IngredientSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Fails startup when the IngredientCode enum and the ingredient_source table disagree. */
@Component
@RequiredArgsConstructor
public class ReferenceCodeCheck implements ApplicationRunner {
    private final IngredientSourceRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        List<ReferenceCodeChecker.Row> rows = this.repository.findAll().stream()
                .map(source -> new ReferenceCodeChecker.Row(source.getCode(), source.getKind(), source.getParentCode()))
                .toList();
        List<String> problems = new ReferenceCodeChecker().check(rows);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("IngredientCode and ingredient_source differ: " + String.join("; ", problems));
        }
    }
}
