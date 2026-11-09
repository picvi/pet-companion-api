package com.picvi.pet_companion_service.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DatabaseSeed {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-01T10:00:00+02:00");

    private final JdbcTemplate jdbc;

    public DatabaseSeed(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID user() {
        UUID id = UUID.randomUUID();
        String name = "user-" + id.toString().substring(0, 8);
        this.jdbc.update("insert into users (id, username, email, password_hash, created_at, updated_at) values (?, ?, ?, 'x', now(), now())",
                id, name, name + "@example.com");
        return id;
    }

    public UUID pet(UUID ownerId) {
        UUID id = UUID.randomUUID();
        Long breedId = this.jdbc.queryForObject("select min(id) from breeds", Long.class);
        this.jdbc.update("insert into pets (id, owner_id, breed_id, name, sex, birth_date, created_at, updated_at) values (?, ?, ?, 'Mila', 'FEMALE', ?, now(), now())",
                id, ownerId, breedId, DAY.minusWeeks(12));
        return id;
    }

    public UUID log(UUID petId, UUID loggedBy) {
        UUID id = UUID.randomUUID();
        this.jdbc.update("""
                insert into pet_log (id, pet_id, client_id, type, local_date, minute_of_day, logged_by, created_at, occurred_at, utc_offset_minutes)
                values (?, ?, ?, 'MEAL', ?, 480, ?, now(), ?, 120)
                """, id, petId, UUID.randomUUID(), DAY, loggedBy, NOW);
        return id;
    }

    public UUID foodProduct(UUID createdBy) {
        UUID id = UUID.randomUUID();
        this.jdbc.update("""
                insert into food_product (id, product_name, form, kind_hint, protein_status, status, normalised_name, created_by, reviewed_by, created_at, updated_at)
                values (?, 'Puppy mix', 'DRY', 'MAIN', 'UNKNOWN', 'OWNER_ADDED', 'puppy mix', ?, ?, now(), now())
                """, id, createdBy, createdBy);
        return id;
    }

    public UUID petFood(UUID petId, UUID productId, UUID createdBy) {
        UUID id = UUID.randomUUID();
        this.jdbc.update("insert into pet_food (id, pet_id, client_id, product_id, kind, started_on, created_by, created_at) values (?, ?, ?, ?, 'MAIN', ?, ?, now())",
                id, petId, UUID.randomUUID(), productId, DAY, createdBy);
        return id;
    }

    public UUID condition(UUID petId, UUID createdBy) {
        UUID id = UUID.randomUUID();
        this.jdbc.update("insert into pet_condition (id, pet_id, kind, certainty, text, created_by, created_at) values (?, ?, 'ALLERGY', 'SUSPECTED', 'chicken', ?, now())",
                id, petId, createdBy);
        return id;
    }

    public void everythingAboutPet(UUID petId, UUID userId) {
        this.jdbc.update("insert into pet_lost_profile (pet_id) values (?)", petId);

        UUID lostReport = UUID.randomUUID();
        this.jdbc.update("""
                insert into lost_report (id, pet_id, client_id, started_at, last_seen_at, last_seen_place, status)
                values (?, ?, ?, ?, ?, 'City park', 'ACTIVE')
                """, lostReport, petId, UUID.randomUUID(), NOW, NOW);
        this.jdbc.update("insert into lost_report_step (report_id, step_code, done_at) values (?, 'SHARE_POSTER', ?)",
                lostReport, NOW);

        UUID checkId = UUID.randomUUID();
        this.jdbc.update("""
                insert into pet_check (id, pet_id, client_id, occurred_on, minute_of_day, source, item, category,
                    substance, context, level, rule_id, rulebook, result, status, created_at, updated_at)
                values (?, ?, ?, ?, 610, 'HOME', 'ATE_SOMETHING', 'EATING', 'CHOCOLATE',
                    '{"sizeCategory":"MEDIUM"}'::jsonb, 'GO_NOW', 'TOXIN_CHOCOLATE', '2026.10.1-draft',
                    '{"review":{"status":"PENDING","testOnly":true}}'::jsonb, 'CLOSED', ?, ?)
                """, checkId, petId, UUID.randomUUID(), DAY, NOW, NOW);
        this.jdbc.update("""
                insert into pet_check_follow_up (id, check_id, client_id, answer, level_after, answered_on, minute_of_day, created_at)
                values (?, ?, ?, 'SAME', 'WATCH', ?, 620, ?)
                """, UUID.randomUUID(), checkId, UUID.randomUUID(), DAY, NOW);

        UUID logId = this.log(petId, userId);
        this.jdbc.update("insert into pet_log_flag (log_id, flag) values (?, 'NEW_FOOD')", logId);
        this.jdbc.update("insert into pet_log_change (log_id, pet_id, change, changed_at) values (?, ?, 'CREATED', ?)", logId, petId, NOW);

        UUID course = UUID.randomUUID();
        this.jdbc.update("""
                insert into medication_course (id, pet_id, client_id, name, amount_hundredths, unit, schedule, with_food, starts_on, start_minute, created_at, updated_at)
                values (?, ?, ?, 'Amoxicillin', 100, 'TABLET', 'DAILY', 'ANY', ?, 480, now(), now())
                """, course, petId, UUID.randomUUID(), DAY);
        this.jdbc.update("insert into medication_dose_time (course_id, minute_of_day) values (?, 480)", course);
        this.jdbc.update("""
                insert into pet_log (id, pet_id, client_id, type, local_date, minute_of_day, logged_by, created_at, occurred_at, utc_offset_minutes, course_id, dose_on, dose_minute)
                values (?, ?, ?, 'MEDICINE', ?, 485, ?, now(), ?, 120, ?, ?, 480)
                """, UUID.randomUUID(), petId, UUID.randomUUID(), DAY, userId, NOW, course, DAY);

        UUID product = this.foodProduct(userId);
        this.jdbc.update("insert into food_product_change (product_id, changed_by, changed_at, reason) values (?, ?, ?, 'CORRECTION')", product, userId, NOW);
        UUID oldFood = this.petFood(petId, product, userId);
        UUID newFood = this.petFood(petId, this.foodProduct(userId), userId);
        this.jdbc.update("insert into pet_log_food (log_id, pet_food_id) values (?, ?)", logId, oldFood);

        UUID transition = UUID.randomUUID();
        this.jdbc.update("""
                insert into food_transition (id, pet_id, client_id, from_food_id, to_food_id, pace, start_on, status, created_at)
                values (?, ?, ?, ?, ?, 'STANDARD', ?, 'ACTIVE', now())
                """, transition, petId, UUID.randomUUID(), oldFood, newFood, DAY);
        this.jdbc.update("insert into food_transition_pause (transition_id, paused_on) values (?, ?)", transition, DAY);
        this.jdbc.update("insert into food_transition_extension (transition_id, step_order, days, created_at, reason_log_id) values (?, 1, 2, ?, ?)", transition, NOW, logId);

        this.condition(petId, userId);
        this.jdbc.update("insert into pet_tried_food (pet_id, food_code, tried_on, reaction, created_by, created_at, updated_at) values (?, 'CARROT', ?, 'FINE', ?, now(), now())", petId, DAY, userId);

        UUID dayEvent = UUID.randomUUID();
        this.jdbc.update("""
                insert into pet_day_event (id, pet_id, type, local_date, start_minute, duration_minutes, recurrence, plan_around, created_at)
                values (?, ?, 'VET', ?, 600, 30, 'NONE', false, now())
                """, dayEvent, petId, DAY);
        this.jdbc.update("insert into pet_day_event_exception (id, event_id, local_date, skipped) values (?, ?, ?, true)", UUID.randomUUID(), dayEvent, DAY);
        this.jdbc.update("""
                insert into pet_vet_visit (id, pet_id, client_id, visited_on, reason, next_check_event_id, created_at, updated_at)
                values (?, ?, ?, ?, 'CHECK_UP', ?, now(), now())
                """, UUID.randomUUID(), petId, UUID.randomUUID(), DAY, dayEvent);

        String careItem = this.jdbc.queryForObject("select min(code) from care_item", String.class);
        this.jdbc.update("insert into care_event (id, pet_id, item_code, performed_on, created_at) values (?, ?, ?, ?, now())", UUID.randomUUID(), petId, careItem, DAY);
        this.jdbc.update("insert into pet_care_setting (id, pet_id, item_code, reminders_enabled) values (?, ?, ?, true)", UUID.randomUUID(), petId, careItem);

        Long activity = this.jdbc.queryForObject("select min(id) from development_activity", Long.class);
        this.jdbc.update("insert into pet_activity_log (id, pet_id, activity_id, performed_on, outcome, created_at) values (?, ?, ?, ?, 'CALM', now())", UUID.randomUUID(), petId, activity, DAY);
        this.jdbc.update("insert into pet_activity_progress (id, pet_id, activity_id, status, created_at, updated_at) values (?, ?, ?, 'IN_PROGRESS', now(), now())", UUID.randomUUID(), petId, activity);

        this.jdbc.update("insert into pet_diet_notice (pet_id, code, seen_on, seen_at) values (?, 'RAW_NOTE', ?, ?)", petId, DAY, NOW);

        UUID settings = UUID.randomUUID();
        this.jdbc.update("insert into pet_rhythm_settings (id, pet_id, wake_minute, bed_minute, created_on) values (?, ?, 420, 1260, ?)", settings, petId, DAY);
        this.jdbc.update("insert into pet_meal_time (settings_id, meal_minute) values (?, 480)", settings);

        this.jdbc.update("insert into refresh_tokens (id, user_id, token_hash, expires_at, created_at) values (?, ?, ?, now() + interval '1 day', now())",
                UUID.randomUUID(), userId, UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
    }
}
