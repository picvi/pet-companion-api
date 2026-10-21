package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FoodSafetySeedTest extends PostgresTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theNineQuickTilesAreNumberedOneToNine() {
        List<Integer> orders = this.jdbc.queryForList("select quick_order from food_safety_item where quick_order is not null order by quick_order", Integer.class);

        assertThat(orders).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void everyFoodHasBothLanguagesAndASource() {
        Integer broken = this.jdbc.queryForObject("""
                select count(*) from food_safety_item
                where btrim(name_en) = '' or btrim(name_pl) = '' or btrim(why_en) = '' or btrim(why_pl) = '' or cardinality(sources) = 0
                """, Integer.class);

        assertThat(broken).isZero();
    }

    @Test
    void noAliasBelongsToTwoFoods() {
        List<Map<String, Object>> shared = this.jdbc.queryForList("""
                select alias, count(distinct code) as foods from (
                    select code, lower(unnest(aliases_en)) as alias from food_safety_item
                    union all
                    select code, lower(unnest(aliases_pl)) as alias from food_safety_item
                ) a group by alias having count(distinct code) > 1
                """);

        assertThat(shared).isEmpty();
    }

    @Test
    void aSearchedWordIsNeverAnotherFoodsName() {
        List<Map<String, Object>> clashes = this.jdbc.queryForList("""
                select a.code, b.code as other from food_safety_item a join food_safety_item b on a.code <> b.code
                where lower(b.name_en) = any (select lower(unnest(a.aliases_en)))
                   or lower(b.name_pl) = any (select lower(unnest(a.aliases_pl)))
                """);

        assertThat(clashes).isEmpty();
    }

    @Test
    void everyLevelIsRepresentedAndAtLeastOneNeverFoodTellsWhatToDo() {
        List<String> levels = this.jdbc.queryForList("select distinct level from food_safety_item", String.class);
        Integer neverWithAdvice = this.jdbc.queryForObject("select count(*) from food_safety_item where level = 'NEVER' and how_en is not null", Integer.class);

        assertThat(levels).containsExactlyInAnyOrder("NEVER", "LITTLE", "YES");
        assertThat(neverWithAdvice).isPositive();
    }

    @Test
    void nothingIsMarkedReviewedBeforeAVetSignsOff() {
        Integer reviewed = this.jdbc.queryForObject("select count(*) from food_safety_item where reviewed_on is not null", Integer.class);

        assertThat(reviewed).isZero();
    }

    @Test
    void theFoodsOwnersAskAboutMostArePresent() {
        List<String> codes = this.jdbc.queryForList("select code from food_safety_item", String.class);

        assertThat(codes).contains("GRAPES", "CHOCOLATE", "CHEESE", "CARROT", "BANANA", "APPLE", "COOKED_CHICKEN", "BREAD",
                "SUGAR_FREE_GUM", "BLUEBERRIES");
    }
}
