package com.picvi.pet_companion_service.development;

import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityCatalogueCsvTest {

    @Test
    void everyRowParsesIntoEnums() {
        List<ActivityDefinition> activities = ActivityCatalogueFixture.load();

        assertThat(activities).hasSize(84);
    }

    @Test
    void everyCategoryBelongsToTheKindOfItsActivity() {
        assertThat(ActivityCatalogueFixture.load())
                .allSatisfy(activity -> assertThat(activity.category().kind()).isEqualTo(activity.kind()));
    }

    @Test
    void codesAreUnique() {
        List<ActivityDefinition> activities = ActivityCatalogueFixture.load();

        assertThat(activities.stream().map(ActivityDefinition::code).distinct()).hasSameSizeAs(activities);
    }
}
