package com.picvi.pet_companion_service.triage.domain;

import java.util.List;
import java.util.Map;

public record CheckCatalog(String version, List<String> categories, List<String> universalFlags,
                           List<Item> items, List<Question> questions, List<String> substances) {
    public CheckCatalog {
        categories = List.copyOf(categories);
        universalFlags = List.copyOf(universalFlags);
        items = List.copyOf(items);
        questions = List.copyOf(questions);
        substances = List.copyOf(substances);
    }

    public Item item(String code) {
        return this.items.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    public Question question(String code) {
        return this.questions.stream().filter(question -> question.code().equals(code)).findFirst().orElse(null);
    }

    public record Item(String code, String category, String kind, List<String> flags, List<String> questions,
                       String related, String briefConcern) {
        public Item {
            flags = List.copyOf(flags);
            questions = List.copyOf(questions);
        }
    }

    public record Question(String code, List<String> options, Map<String, Object> prefill) {
        public Question {
            options = List.copyOf(options);
            prefill = Map.copyOf(prefill);
        }
    }
}
