package com.picvi.pet_companion_service.triage.domain;

import com.picvi.pet_companion_service.triage.service.CatalogLoader;
import com.picvi.pet_companion_service.triage.service.RuleBookLoader;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TriageEngineTest {
    @Test
    void anyUniversalRedFlagWinsForEveryItem() {
        var resources = new DefaultResourceLoader();
        var catalog = new CatalogLoader(resources);
        catalog.load();
        var loader = new RuleBookLoader(resources, catalog, false);
        loader.load();
        var engine = new TriageEngine(new ConditionEvaluator(), loader.getRuleBook());
        var facts = new CheckFacts("ATE_SOMETHING", Set.of("COLLAPSED"), Map.of(), null,
                new Context(10, "SMALL", "POODLE_MINIATURE", 1, 3, VaccineHistory.KNOWN,
                        null, null, 0, 0, 0, null), null);

        assertThat(engine.evaluate(facts).winner().id()).isEqualTo("RED_FLAG_COLLAPSED");
        assertThat(engine.evaluate(facts).level()).isEqualTo(Level.GO_NOW);
        assertThat(engine.evaluate(facts).winner().review().status()).isEqualTo("PENDING");
    }
}
