package com.picvi.pet_companion_service.triage.service;

import com.picvi.pet_companion_service.triage.domain.CheckFacts;
import com.picvi.pet_companion_service.triage.domain.ConditionEvaluator;
import com.picvi.pet_companion_service.triage.domain.Context;
import com.picvi.pet_companion_service.triage.domain.TriageEngine;
import com.picvi.pet_companion_service.triage.domain.VaccineHistory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBookLoaderTest {
    @Test
    void loadsPendingRulesForLocalTesting() {
        DefaultResourceLoader resources = new DefaultResourceLoader();
        CatalogLoader catalogLoader = new CatalogLoader(resources);
        catalogLoader.load();
        RuleBookLoader loader = new RuleBookLoader(resources, catalogLoader, false);
        loader.load();

        assertThat(loader.getRuleBook().version()).isEqualTo("2026.10.1-draft");
        assertThat(loader.getRuleBook().rules()).hasSize(17)
                .allMatch(rule -> rule.review().status().equals("PENDING"));

        TriageEngine engine = new TriageEngine(new ConditionEvaluator(), loader.getRuleBook());
        var facts = new CheckFacts("ATE_SOMETHING", Set.of(), Map.of(), "CHOCOLATE",
                new Context(24, "MEDIUM", "LABRADOR", 2, 3, VaccineHistory.KNOWN,
                        null, null, 0, 0, 0, null), null);
        var evaluation = engine.evaluate(facts);
        assertThat(evaluation.level().name()).isEqualTo("GO_NOW");
        assertThat(evaluation.winner().review().status()).isEqualTo("PENDING");
    }

    @Test
    void rejectsPendingRulesWhenProductionReviewIsRequired() {
        DefaultResourceLoader resources = new DefaultResourceLoader();
        CatalogLoader catalogLoader = new CatalogLoader(resources);
        catalogLoader.load();
        RuleBookLoader loader = new RuleBookLoader(resources, catalogLoader, true);

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not vet reviewed");
    }
}
