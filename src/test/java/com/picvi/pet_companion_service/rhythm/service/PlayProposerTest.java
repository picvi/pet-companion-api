package com.picvi.pet_companion_service.rhythm.service;

import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.FocusReason;
import com.picvi.pet_companion_service.development.domain.FocusType;
import com.picvi.pet_companion_service.development.domain.PlanReason;
import com.picvi.pet_companion_service.development.dto.ActivityRefDto;
import com.picvi.pet_companion_service.development.dto.FocusDto;
import com.picvi.pet_companion_service.development.dto.PlanItemDto;
import com.picvi.pet_companion_service.development.dto.TodayResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayProposerTest {
    private static final ActivityRefDto SIT = skill("SIT", false);
    private static final ActivityRefDto NAME = skill("NAME", false);
    private static final ActivityRefDto ALONE = skill("ALONE_FIRST_SECONDS", true);
    private static final ActivityRefDto MEET = new ActivityRefDto("MEET_PEOPLE", ActivityKind.SOCIALIZATION,
            ActivityCategory.values()[0], 1, ActivityCategory.values()[0].group(), false);

    private final PlayProposer proposer = new PlayProposer();

    @Test
    void proposesTheSkillThatIsTodaysFocus() {
        TodayResponse today = today(focus(SIT, false), List.of(item(NAME, false)));

        assertThat(this.proposer.propose(today)).contains(SIT);
    }

    @Test
    void movesOnToThePlanWhenTheFocusIsDoneOrNotASkill() {
        assertThat(this.proposer.propose(today(focus(SIT, true), List.of(item(NAME, false))))).contains(NAME);
        assertThat(this.proposer.propose(today(focus(MEET, false), List.of(item(NAME, false))))).contains(NAME);
    }

    @Test
    void neverProposesAnAloneTimeActivityForPlay() {
        assertThat(this.proposer.propose(today(focus(ALONE, false), List.of(item(ALONE, false))))).isEmpty();
    }

    @Test
    void proposesNothingWhenEverythingIsDone() {
        assertThat(this.proposer.propose(today(focus(SIT, true), List.of(item(NAME, true))))).isEmpty();
    }

    @Test
    void proposesNothingWhenThereIsNoFocus() {
        assertThat(this.proposer.propose(today(null, List.of()))).isEmpty();
    }

    private static ActivityRefDto skill(String code, boolean alone) {
        return new ActivityRefDto(code, ActivityKind.SKILL, ActivityCategory.values()[0], 1,
                ActivityCategory.values()[0].group(), alone);
    }

    private static FocusDto focus(ActivityRefDto activity, boolean done) {
        return new FocusDto(FocusType.ACTIVITY, activity, null, done);
    }

    private static PlanItemDto item(ActivityRefDto activity, boolean done) {
        return new PlanItemDto(activity, null, done);
    }

    private static TodayResponse today(FocusDto focus, List<PlanItemDto> plan) {
        return new TodayResponse(LocalDate.of(2026, 10, 14), "Mila", 11, null, focus, plan, false, false);
    }
}
