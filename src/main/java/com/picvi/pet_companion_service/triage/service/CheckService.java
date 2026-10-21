package com.picvi.pet_companion_service.triage.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.exception.triage.InvalidCheck;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.triage.domain.CheckCatalog;
import com.picvi.pet_companion_service.triage.domain.CheckFacts;
import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.domain.Context;
import com.picvi.pet_companion_service.triage.domain.Evaluation;
import com.picvi.pet_companion_service.triage.domain.FactName;
import com.picvi.pet_companion_service.triage.domain.Level;
import com.picvi.pet_companion_service.triage.domain.Rule;
import com.picvi.pet_companion_service.triage.domain.TriageEngine;
import com.picvi.pet_companion_service.triage.dto.CheckContextResponse;
import com.picvi.pet_companion_service.triage.dto.CheckResponse;
import com.picvi.pet_companion_service.triage.dto.CreateCheckRequest;
import com.picvi.pet_companion_service.triage.entity.PetCheck;
import com.picvi.pet_companion_service.triage.repository.PetCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckService {
    private final PetService petService;
    private final CheckContextService contextService;
    private final CheckReadService readService;
    private final PetCheckRepository checkRepository;
    private final TriageEngine triageEngine;
    private final CatalogLoader catalogLoader;
    private final RuleBookLoader ruleBookLoader;
    private final ClientDateValidator clientDateValidator;
    private final Clock clock;

    @Transactional
    public CreateCheckResult create(UUID ownerId, UUID petId, CreateCheckRequest request) {
        this.clientDateValidator.validate(request.date());
        this.validateOccurredOn(request.date(), request.occurredOn());
        this.petService.getOwnedPetProfile(ownerId, petId);
        CheckCatalog catalog = this.catalogLoader.getCatalog();
        CheckCatalog.Item item = catalog.item(request.item());
        if (item == null) throw new InvalidCheck("item", "INVALID");
        this.validateRequest(catalog, item, request);

        var existing = this.checkRepository.findByPetIdAndClientId(petId, request.clientId());
        if (existing.isPresent()) {
            return new CreateCheckResult(this.readService.get(ownerId, petId, existing.get().getId()), false);
        }

        CheckContextResponse assembled = this.contextService.get(ownerId, petId, item.code(), request.date());
        Context context = this.context(assembled);
        CheckFacts facts = new CheckFacts(item.code(), Set.copyOf(request.flags()), request.answers(), request.substance(), context, null);
        Evaluation evaluation;
        try {
            evaluation = this.triageEngine.evaluate(facts);
        } catch (IllegalStateException noRule) {
            throw new InvalidCheck("item", "NO_RULE_MATCH");
        }
        Rule rule = evaluation.winner();
        Instant now = Instant.now(this.clock);
        Instant dueAt = rule.checkBackMinutes() == null ? null : now.plus(Duration.ofMinutes(rule.checkBackMinutes()));
        CheckStatus status = rule.level() == Level.WATCH ? CheckStatus.WATCHING : CheckStatus.CLOSED;
        Map<String, Object> contextSnapshot = this.contextMap(context);
        Map<String, Object> result = this.result(rule, context, status, dueAt);
        PetCheck check = new PetCheck(petId, request.clientId(), request.occurredOn(), request.minuteOfDay(), request.source(),
                item.code(), item.category(), Set.copyOf(request.flags()), request.answers(), request.substance(), request.note(),
                contextSnapshot, rule.level(), rule.id(), this.ruleBookLoader.getRuleBook().version(), result, status, dueAt, now);
        PetCheck saved = this.checkRepository.saveAndFlush(check);
        return new CreateCheckResult(this.readService.get(ownerId, petId, saved.getId()), true);
    }

    @Transactional
    public void delete(UUID ownerId, UUID petId, UUID checkId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetCheck check = this.checkRepository.findByIdAndPetId(checkId, petId).orElseThrow(PetNotFound::new);
        this.checkRepository.delete(check);
    }

    private void validateRequest(CheckCatalog catalog, CheckCatalog.Item item, CreateCheckRequest request) {
        if (new HashSet<>(request.flags()).size() != request.flags().size()) {
            throw new InvalidCheck("flags", "DUPLICATE");
        }
        Set<String> allowedFlags = new HashSet<>(catalog.universalFlags());
        allowedFlags.addAll(item.flags());
        if (!allowedFlags.containsAll(request.flags())) throw new InvalidCheck("flags", "INVALID");
        if (request.flags().isEmpty() && !item.code().equals("ATE_SOMETHING")) {
            Set<String> required = Set.copyOf(item.questions());
            if (!request.answers().keySet().containsAll(required)) throw new InvalidCheck("answers", "MISSING");
            if (!required.containsAll(request.answers().keySet())) throw new InvalidCheck("answers", "INVALID");
            for (String questionCode : required) {
                CheckCatalog.Question question = catalog.question(questionCode);
                if (!question.options().contains(request.answers().get(questionCode))) {
                    throw new InvalidCheck("answers", "INVALID");
                }
            }
        }
        if (item.code().equals("ATE_SOMETHING")) {
            if (request.substance() == null || request.substance().isBlank()) throw new InvalidCheck("substance", "MISSING");
            if (!catalog.substances().contains(request.substance())) throw new InvalidCheck("substance", "INVALID");
            if (!request.answers().isEmpty()) throw new InvalidCheck("answers", "INVALID");
        } else if (request.substance() != null) {
            throw new InvalidCheck("substance", "INVALID");
        }
        if (request.note() != null && !item.code().equals("OTHER")) throw new InvalidCheck("note", "INVALID");
        if (request.note() != null && request.note().length() > 500) throw new InvalidCheck("note", "TOO_LONG");
    }

    private void validateOccurredOn(LocalDate date, LocalDate occurredOn) {
        if (occurredOn == null || occurredOn.isAfter(date) || occurredOn.isBefore(date.minusDays(7))) {
            throw new InvalidCheck("occurredOn", "OUT_OF_RANGE");
        }
    }

    private Context context(CheckContextResponse response) {
        return new Context(response.ageWeeks(), response.sizeCategory().name(), response.breedCode(), response.dhppiDoses(),
                response.dhppiTotal(), response.vaccineHistory(), response.daysSinceFoodChange(), response.hoursSinceVaccine(),
                response.related().count24h(), response.related().count7d(), null, null);
    }

    private Map<String, Object> contextMap(Context context) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ageWeeks", context.ageWeeks());
        map.put("sizeCategory", context.sizeCategory());
        map.put("breedCode", context.breedCode());
        map.put("dhppiDoses", context.dhppiDoses());
        map.put("dhppiTotal", context.dhppiTotal());
        map.put("vaccineHistory", context.vaccineHistory().name());
        map.put("daysSinceFoodChange", context.daysSinceFoodChange());
        map.put("hoursSinceVaccine", context.hoursSinceVaccine());
        map.put("relatedLogs24h", context.relatedLogs24h());
        map.put("relatedLogs7d", context.relatedLogs7d());
        map.put("relatedChecks24h", context.relatedChecks24h());
        map.put("awakeMinutes", context.awakeMinutes());
        return map;
    }

    private Map<String, Object> result(Rule rule, Context context, CheckStatus status, Instant dueAt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reason", Map.of("code", rule.reason(), "params", this.params(rule.reasonParams(), context)));
        List<Map<String, Object>> steps = new ArrayList<>();
        for (Rule.RuleStep step : rule.steps()) {
            steps.add(Map.of("code", step.code(), "params", this.params(step.params(), context)));
        }
        result.put("steps", steps);
        result.put("changeIf", Map.of("to", nextLevel(rule.level()), "codes", rule.changeIf()));
        result.put("actions", rule.actions().stream()
                .map(action -> Map.<String, Object>of("code", action.code(), "params", action.params())).toList());
        result.put("checkBack", dueAt == null ? null : Map.of("afterMinutes", rule.checkBackMinutes(), "dueAt", dueAt.toString()));
        result.put("status", status.name());
        result.put("review", Map.of("status", rule.review().status(), "testOnly", !"REVIEWED".equals(rule.review().status())));
        result.put("caseIds", rule.caseIds());
        return result;
    }

    private Map<String, Object> params(Map<String, FactName> parameterNames, Context context) {
        Map<String, Object> params = new LinkedHashMap<>();
        parameterNames.forEach((name, fact) -> {
            Object value = switch (fact) {
                case AGE_WEEKS -> context.ageWeeks();
                case SIZE_CATEGORY -> context.sizeCategory();
                case BREED_CODE -> context.breedCode();
                case DHPPI_DOSES -> context.dhppiDoses();
                case DHPPI_TOTAL -> context.dhppiTotal();
                case VACCINE_HISTORY -> context.vaccineHistory().name();
                case DAYS_SINCE_FOOD_CHANGE -> context.daysSinceFoodChange();
                case HOURS_SINCE_VACCINE -> context.hoursSinceVaccine();
                case RELATED_LOGS_24H -> context.relatedLogs24h();
                case RELATED_LOGS_7D -> context.relatedLogs7d();
                case RELATED_CHECKS_24H -> context.relatedChecks24h();
                case AWAKE_MINUTES -> context.awakeMinutes();
            };
            if (value != null) params.put(name, value);
        });
        return params;
    }

    private static Level nextLevel(Level level) {
        return level == Level.GO_NOW ? Level.GO_NOW : Level.values()[level.ordinal() + 1];
    }

    public record CreateCheckResult(CheckResponse response, boolean created) {
    }
}
