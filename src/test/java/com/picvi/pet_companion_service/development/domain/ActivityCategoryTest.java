package com.picvi.pet_companion_service.development.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityCategoryTest {

    @Test
    void everyCategoryHasAKind() {
        assertThat(Arrays.stream(ActivityCategory.values()).map(ActivityCategory::kind)).doesNotContainNull();
    }

    @Test
    void everyCategoryHasAGroup() {
        assertThat(Arrays.stream(ActivityCategory.values()).map(ActivityCategory::group)).doesNotContainNull();
    }

    @Test
    void categoriesMapToDocumentedGroups() {
        assertThat(groupOf(ActivityCategory.SOUNDS, ActivityCategory.SURFACES, ActivityCategory.OBJECTS,
                ActivityCategory.HOME_LIFE)).isEqualTo(ActivityGroup.AT_HOME);
        assertThat(groupOf(ActivityCategory.PEOPLE, ActivityCategory.DOGS,
                ActivityCategory.OTHER_ANIMALS)).isEqualTo(ActivityGroup.MEETING_OTHERS);
        assertThat(groupOf(ActivityCategory.PLACES, ActivityCategory.TRAVEL)).isEqualTo(ActivityGroup.OUT_AND_ABOUT);
        assertThat(groupOf(ActivityCategory.FOUNDATION_CUES, ActivityCategory.LEAD_WALKING,
                ActivityCategory.MOUTH_MANNERS, ActivityCategory.IMPULSE_CONTROL)).isEqualTo(ActivityGroup.TRAINING);
        assertThat(groupOf(ActivityCategory.CARE, ActivityCategory.HANDLING)).isEqualTo(ActivityGroup.CARE);
    }

    private static ActivityGroup groupOf(ActivityCategory first, ActivityCategory... others) {
        ActivityGroup group = first.group();
        for (ActivityCategory other : others) {
            assertThat(other.group()).isEqualTo(group);
        }
        return group;
    }

    @Test
    void socialisationRotationOrderIsDocumentedOrder() {
        assertThat(ActivityCategory.rotationOrder(ActivityKind.SOCIALIZATION)).containsExactly(
                ActivityCategory.HANDLING, ActivityCategory.SOUNDS, ActivityCategory.PEOPLE,
                ActivityCategory.SURFACES, ActivityCategory.PLACES, ActivityCategory.OBJECTS,
                ActivityCategory.TRAVEL, ActivityCategory.OTHER_ANIMALS, ActivityCategory.DOGS);
    }

    @Test
    void skillRotationOrderIsDocumentedOrder() {
        assertThat(ActivityCategory.rotationOrder(ActivityKind.SKILL)).containsExactly(
                ActivityCategory.HOME_LIFE, ActivityCategory.FOUNDATION_CUES, ActivityCategory.LEAD_WALKING,
                ActivityCategory.MOUTH_MANNERS, ActivityCategory.IMPULSE_CONTROL, ActivityCategory.CARE);
    }
}
