package com.picvi.pet_companion_service.triage.domain;

import java.util.List;
import java.util.Set;

public sealed interface Condition permits Condition.All, Condition.Any, Condition.Not, Condition.Fact,
        Condition.Answer, Condition.HasFlag, Condition.Substance {
    record All(List<Condition> conditions) implements Condition {
        public All { conditions = List.copyOf(conditions); }
    }

    record Any(List<Condition> conditions) implements Condition {
        public Any { conditions = List.copyOf(conditions); }
    }

    record Not(Condition condition) implements Condition {
    }

    record Fact(FactName name, Op op, Object value) implements Condition {
    }

    record Answer(String question, Op op, Set<String> values) implements Condition {
        public Answer { values = Set.copyOf(values); }
    }

    record HasFlag(String flag) implements Condition {
    }

    record Substance(String code) implements Condition {
    }
}
