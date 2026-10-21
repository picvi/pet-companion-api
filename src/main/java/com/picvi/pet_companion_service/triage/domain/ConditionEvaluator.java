package com.picvi.pet_companion_service.triage.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ConditionEvaluator {

    public boolean matches(Condition condition, CheckFacts facts) {
        return switch (condition) {
            case Condition.All all -> all.conditions().stream().allMatch(child -> this.matches(child, facts));
            case Condition.Any any -> any.conditions().stream().anyMatch(child -> this.matches(child, facts));
            case Condition.Not not -> !this.matches(not.condition(), facts);
            case Condition.HasFlag hasFlag -> facts.flags().contains(hasFlag.flag());
            case Condition.Answer answer -> this.matches(facts.answers().get(answer.question()), answer.op(), answer.values());
            case Condition.Fact fact -> this.matches(this.value(facts.context(), fact.name()), fact.op(), fact.value());
            case Condition.Substance substance -> Objects.equals(facts.substance(), substance.code());
        };
    }

    private boolean matches(Object actual, Op op, Object expected) {
        if (actual == null) {
            return false;
        }
        if (op == Op.IN) {
            Collection<?> values = expected instanceof Collection<?> collection ? collection : List.of(expected);
            return values.stream().anyMatch(value -> this.equal(actual, value));
        }
        int compared = this.compare(actual, expected);
        return switch (op) {
            case LT -> compared < 0;
            case LTE -> compared <= 0;
            case GT -> compared > 0;
            case GTE -> compared >= 0;
            case EQ -> this.equal(actual, expected);
            case IN -> false;
        };
    }

    private boolean equal(Object actual, Object expected) {
        if (actual instanceof Enum<?> enumValue) {
            return enumValue.name().equals(Objects.toString(expected));
        }
        if (actual instanceof Number left && expected instanceof Number right) {
            return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) == 0;
        }
        return Objects.equals(actual, expected);
    }

    private int compare(Object actual, Object expected) {
        if (actual instanceof Number left && expected instanceof Number right) {
            return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString()));
        }
        if (actual instanceof Comparable<?> comparable && actual.getClass().isInstance(expected)) {
            @SuppressWarnings("unchecked")
            Comparable<Object> typed = (Comparable<Object>) comparable;
            return typed.compareTo(expected);
        }
        throw new IllegalArgumentException("Condition values are not comparable: " + actual.getClass().getSimpleName());
    }

    private Object value(Context context, FactName name) {
        return switch (name) {
            case AGE_WEEKS -> context.ageWeeks();
            case SIZE_CATEGORY -> context.sizeCategory();
            case BREED_CODE -> context.breedCode();
            case DHPPI_DOSES -> context.dhppiDoses();
            case DHPPI_TOTAL -> context.dhppiTotal();
            case VACCINE_HISTORY -> context.vaccineHistory();
            case DAYS_SINCE_FOOD_CHANGE -> context.daysSinceFoodChange();
            case HOURS_SINCE_VACCINE -> context.hoursSinceVaccine();
            case RELATED_LOGS_24H -> context.relatedLogs24h();
            case RELATED_LOGS_7D -> context.relatedLogs7d();
            case RELATED_CHECKS_24H -> context.relatedChecks24h();
            case AWAKE_MINUTES -> context.awakeMinutes();
        };
    }
}
