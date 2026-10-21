package com.picvi.pet_companion_service.brief.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class QuestionSuggester {

    public record Question(String code, Map<String, String> params) {
        static Question of(String code) {
            return new Question(code, null);
        }
    }

    public record VaccineDue(String code, LocalDate dueOn) {
    }

    /** {@code concern} is null when the owner named none. */
    public List<Question> suggest(Concern concern, List<LocalDate> foodChangeDates, List<VaccineDue> vaccines, LocalDate date) {
        List<Question> questions = new ArrayList<>();
        if (concern == null || concern == Concern.OTHER) {
            questions.add(Question.of("GENERAL_HEALTH_CHECK"));
            questions.add(Question.of("GENERAL_WEIGHT_GROWTH"));
            questions.add(Question.of("GENERAL_NEXT_STEPS"));
        } else {
            questions.add(Question.of(concern.name() + "_CAUSE"));
            questions.add(Question.of(concern.name() + "_WHEN_URGENT"));
            questions.add(Question.of(concern.name() + "_TESTS"));
        }
        LocalDate foodChangeSince = date.minusDays(BriefLimits.RECENT_FOOD_CHANGE_DAYS);
        if (foodChangeDates.stream().anyMatch(day -> !day.isBefore(foodChangeSince) && !day.isAfter(date))) {
            questions.add(Question.of("FOOD_CHANGED_RECENTLY"));
        }
        LocalDate vaccineSoon = date.plusDays(BriefLimits.VACCINE_SOON_DAYS);
        vaccines.stream()
                .filter(vaccine -> vaccine.dueOn() != null && !vaccine.dueOn().isAfter(vaccineSoon))
                .min(Comparator.comparing(VaccineDue::dueOn))
                .ifPresent(vaccine -> questions.add(new Question("VACCINE_DUE_SOON", Map.of("vaccineCode", vaccine.code()))));
        return questions;
    }
}
