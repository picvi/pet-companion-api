package com.picvi.pet_companion_service.brief.domain;

import com.picvi.pet_companion_service.brief.domain.QuestionSuggester.Question;
import com.picvi.pet_companion_service.brief.domain.QuestionSuggester.VaccineDue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionSuggesterTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 15);

    private final QuestionSuggester suggester = new QuestionSuggester();

    @Test
    void aConcernGetsItsTrio() {
        assertThat(codes(this.suggester.suggest(Concern.DIARRHOEA, List.of(), List.of(), DATE)))
                .containsExactly("DIARRHOEA_CAUSE", "DIARRHOEA_WHEN_URGENT", "DIARRHOEA_TESTS");
    }

    @Test
    void noConcernGetsTheGeneralTrio() {
        assertThat(codes(this.suggester.suggest(null, List.of(), List.of(), DATE)))
                .containsExactly("GENERAL_HEALTH_CHECK", "GENERAL_WEIGHT_GROWTH", "GENERAL_NEXT_STEPS");
    }

    @Test
    void foodChangeSixDaysAgoCountsAndEightDoesNot() {
        assertThat(codes(this.suggester.suggest(null, List.of(DATE.minusDays(6)), List.of(), DATE))).contains("FOOD_CHANGED_RECENTLY");
        assertThat(codes(this.suggester.suggest(null, List.of(DATE.minusDays(8)), List.of(), DATE))).doesNotContain("FOOD_CHANGED_RECENTLY");
    }

    @Test
    void vaccineDueInSevenDaysCountsAndEightDoesNot() {
        List<Question> soon = this.suggester.suggest(null, List.of(), List.of(new VaccineDue("DHPPI_2", DATE.plusDays(7))), DATE);
        List<Question> later = this.suggester.suggest(null, List.of(), List.of(new VaccineDue("DHPPI_2", DATE.plusDays(8))), DATE);

        assertThat(soon).contains(new Question("VACCINE_DUE_SOON", Map.of("vaccineCode", "DHPPI_2")));
        assertThat(codes(later)).doesNotContain("VACCINE_DUE_SOON");
    }

    private static List<String> codes(List<Question> questions) {
        return questions.stream().map(Question::code).toList();
    }
}
