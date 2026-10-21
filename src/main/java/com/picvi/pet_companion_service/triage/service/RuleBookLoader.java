package com.picvi.pet_companion_service.triage.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.picvi.pet_companion_service.triage.domain.Condition;
import com.picvi.pet_companion_service.triage.domain.CheckCatalog;
import com.picvi.pet_companion_service.triage.domain.FactName;
import com.picvi.pet_companion_service.triage.domain.Level;
import com.picvi.pet_companion_service.triage.domain.Op;
import com.picvi.pet_companion_service.triage.domain.Rule;
import com.picvi.pet_companion_service.triage.domain.RuleBook;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RuleBookLoader {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final ResourceLoader resourceLoader;
    private final CatalogLoader catalogLoader;
    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final boolean requireReviewed;

    @Getter
    private RuleBook ruleBook;

    public RuleBookLoader(ResourceLoader resourceLoader, CatalogLoader catalogLoader,
                          @Value("${pawstep.triage.require-reviewed:false}") boolean requireReviewed) {
        this.resourceLoader = resourceLoader;
        this.catalogLoader = catalogLoader;
        this.requireReviewed = requireReviewed;
    }

    @PostConstruct
    public void load() {
        try {
            Map<String, Object> sourceDocument = this.read("classpath:triage/sources.yml");
            Set<String> knownSources = new HashSet<>();
            for (Object source : this.list(sourceDocument.get("sources"))) {
                knownSources.add(source instanceof Map<?, ?> sourceMap
                        ? this.string(this.map(sourceMap).get("id")) : this.string(source));
            }
            Map<String, Object> rulesDocument = this.read("classpath:triage/rules/rulebook.yml");
            List<Rule> rules = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (Object rawRule : this.list(rulesDocument.get("rules"))) {
                Rule rule = this.parseRule(this.map(rawRule));
                this.validate(rule, knownSources, this.catalogLoader.getCatalog());
                if (!ids.add(rule.id())) {
                    throw new IllegalStateException("Duplicate triage rule id: " + rule.id());
                }
                rules.add(rule);
            }
            this.ruleBook = new RuleBook(this.string(rulesDocument.get("version")), rules);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load triage rulebook: " + exception.getMessage(), exception);
        }
    }

    private void validate(Rule rule, Set<String> knownSources, CheckCatalog catalog) {
        if (rule.sources().isEmpty() || !knownSources.containsAll(rule.sources())) {
            throw new IllegalStateException("Rule " + rule.id() + " has no source or references an unknown source");
        }
        if (rule.review() == null || !Set.of("PENDING", "REVIEWED").contains(rule.review().status())) {
            throw new IllegalStateException("Rule " + rule.id() + " has an invalid review status");
        }
        if (this.requireReviewed && !"REVIEWED".equals(rule.review().status())) {
            throw new IllegalStateException("Production triage rule " + rule.id() + " is not vet reviewed");
        }
        if ("REVIEWED".equals(rule.review().status())
                && (blank(rule.review().reviewer()) || blank(rule.review().reviewedOn()))) {
            throw new IllegalStateException("Reviewed rule " + rule.id() + " must name the reviewer and date");
        }
        if (rule.redFlag() && rule.level() != Level.GO_NOW && rule.level() != Level.CALL_TODAY) {
            throw new IllegalStateException("Red-flag rule " + rule.id() + " cannot be below CALL_TODAY");
        }
        if (rule.steps().isEmpty() || rule.steps().size() > 4) {
            throw new IllegalStateException("Rule " + rule.id() + " must have one to four steps");
        }
        if (rule.level() == Level.WATCH && rule.checkBackMinutes() == null) {
            throw new IllegalStateException("WATCH rule " + rule.id() + " must define a check-back interval");
        }
        if (rule.level() != Level.WATCH && rule.checkBackMinutes() != null) {
            throw new IllegalStateException("Only WATCH rules can define a check-back interval: " + rule.id());
        }
        if (!rule.items().contains("ANY") && rule.items().stream().anyMatch(item -> catalog.item(item) == null)) {
            throw new IllegalStateException("Rule " + rule.id() + " references an unknown catalogue item");
        }
        if (!catalog.substances().containsAll(this.substancesIn(rule.when()))) {
            throw new IllegalStateException("Rule " + rule.id() + " references an unknown substance");
        }
        if (!catalog.universalFlags().containsAll(this.flagsIn(rule.when()))) {
            throw new IllegalStateException("Rule " + rule.id() + " references an unknown universal flag");
        }
    }

    private Set<String> flagsIn(Condition condition) {
        return switch (condition) {
            case Condition.HasFlag flag -> Set.of(flag.flag());
            case Condition.All all -> all.conditions().stream().map(this::flagsIn).flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
            case Condition.Any any -> any.conditions().stream().map(this::flagsIn).flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
            case Condition.Not not -> this.flagsIn(not.condition());
            default -> Set.of();
        };
    }

    private Set<String> substancesIn(Condition condition) {
        return switch (condition) {
            case Condition.Substance substance -> Set.of(substance.code());
            case Condition.All all -> all.conditions().stream().map(this::substancesIn).flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
            case Condition.Any any -> any.conditions().stream().map(this::substancesIn).flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
            case Condition.Not not -> this.substancesIn(not.condition());
            default -> Set.of();
        };
    }

    private Rule parseRule(Map<String, Object> raw) {
        Map<String, Object> review = raw.containsKey("review") ? this.map(raw.get("review")) : Map.of();
        List<Rule.RuleStep> steps = new ArrayList<>();
        for (Object rawStep : this.list(raw.get("steps"))) {
            Map<String, Object> step = this.map(rawStep);
            steps.add(new Rule.RuleStep(this.string(step.get("code")), this.factParams(step.get("params"))));
        }
        List<Rule.RuleAction> actions = new ArrayList<>();
        for (Object rawAction : this.list(raw.get("actions"))) {
            Map<String, Object> action = this.map(rawAction);
            actions.add(new Rule.RuleAction(this.string(action.get("code")), this.stringMap(action.get("params"))));
        }
        Rule.RuleReview ruleReview = new Rule.RuleReview(this.string(review.getOrDefault("status", "PENDING")),
                nullableString(review.get("reviewer")), nullableString(review.get("reviewedOn")));
        return new Rule(this.string(raw.get("id")), Set.copyOf(this.stringList(raw.get("items"))),
                this.parseCondition(this.map(raw.get("when"))), Level.valueOf(this.string(raw.get("level"))),
                number(raw.getOrDefault("priority", 0)), this.string(raw.get("reason")),
                this.factParams(raw.get("reasonParams")), steps, this.stringList(raw.get("changeIf")), actions,
                raw.get("checkBackMinutes") == null ? null : number(raw.get("checkBackMinutes")),
                nullableString(raw.get("briefConcern")), this.stringList(raw.get("caseIds")),
                this.stringList(raw.get("sources")), ruleReview, Boolean.TRUE.equals(raw.get("redFlag")));
    }

    private Condition parseCondition(Map<String, Object> raw) {
        if (raw.containsKey("all")) {
            return new Condition.All(this.list(raw.get("all")).stream().map(this::map)
                    .map(this::parseCondition).toList());
        }
        if (raw.containsKey("any")) {
            return new Condition.Any(this.list(raw.get("any")).stream().map(this::map)
                    .map(this::parseCondition).toList());
        }
        if (raw.containsKey("not")) {
            return new Condition.Not(this.parseCondition(this.map(raw.get("not"))));
        }
        if (raw.containsKey("flag")) {
            return new Condition.HasFlag(this.string(raw.get("flag")));
        }
        if (raw.containsKey("substance")) {
            return new Condition.Substance(this.string(raw.get("substance")));
        }
        if (raw.containsKey("fact")) {
            Map<String, Object> fact = this.map(raw.get("fact"));
            return new Condition.Fact(FactName.valueOf(this.string(fact.get("name"))),
                    Op.valueOf(this.string(fact.get("op"))), fact.get("value"));
        }
        if (raw.containsKey("answer")) {
            Map<String, Object> answer = this.map(raw.get("answer"));
            return new Condition.Answer(this.string(answer.get("question")), Op.valueOf(this.string(answer.get("op"))),
                    Set.copyOf(this.stringList(answer.get("values"))));
        }
        throw new IllegalStateException("Unknown condition shape: " + raw.keySet());
    }

    private Map<String, FactName> factParams(Object raw) {
        Map<String, FactName> params = new LinkedHashMap<>();
        this.map(raw).forEach((key, value) -> params.put(key, FactName.valueOf(this.string(value))));
        return params;
    }

    private Map<String, String> stringMap(Object raw) {
        Map<String, String> result = new LinkedHashMap<>();
        this.map(raw).forEach((key, value) -> result.put(key, this.string(value)));
        return result;
    }

    private Map<String, Object> read(String location) throws IOException {
        Resource resource = this.resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing rule resource " + location);
        }
        return this.yamlMapper.readValue(resource.getInputStream(), MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> raw)) throw new IllegalStateException("Expected a YAML mapping");
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, child) -> result.put(String.valueOf(key), child));
        return result;
    }

    private List<?> list(Object value) {
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) throw new IllegalStateException("Expected a YAML list");
        return list;
    }

    private List<String> stringList(Object value) {
        return this.list(value).stream().map(this::string).toList();
    }

    private String string(Object value) {
        if (value == null) throw new IllegalStateException("Required YAML value is missing");
        return String.valueOf(value);
    }

    private static String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int number(Object value) {
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
