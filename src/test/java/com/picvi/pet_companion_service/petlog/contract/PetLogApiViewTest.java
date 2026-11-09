package com.picvi.pet_companion_service.petlog.contract;

import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogFood;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.petlog.repository.PetLogRepository;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PetLogApiViewTest extends PostgresTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 10);
    private static final List<String> GENERATED_FIELDS = List.of("id", "clientId", "loggedBy", "createdAt", "updatedAt");

    @Autowired
    private PetLogRepository repository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JsonMapper mapper;

    @Test
    void everyLogTypeIsServedWithTheSameJsonAfterAReload() throws IOException {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        UUID user = seed.user();
        UUID pet = seed.pet(user);
        UUID food = seed.petFood(pet, seed.foodProduct(user), user);

        this.save(pet, user, LogType.TOILET, LogDetail.POO, 400, new LogValues(false, 6, Set.of(LogFlag.BLOOD, LogFlag.MUCUS), null, null, null, null, null, null, "loose"));
        this.save(pet, user, LogType.TOILET, LogDetail.PEE, 410, new LogValues(true, null, null, null, null, null, null, null, null, null));
        this.save(pet, user, LogType.MEAL, LogDetail.LEFT_SOME, 480, new LogValues(null, null, Set.of(LogFlag.NEW_FOOD), null, null, null, null, "Puppy mix", null, null, List.of(new LogFood(food))));
        this.save(pet, user, LogType.SLEEP, null, 500, new LogValues(null, null, null, null, null, null, null, null, null, "nap"));
        this.save(pet, user, LogType.WAKE, null, 520, LogValues.none());
        this.save(pet, user, LogType.WALK, null, 540, new LogValues(null, null, null, null, null, 40, null, null, null, null));
        this.save(pet, user, LogType.WEIGHT, null, 560, new LogValues(null, null, null, null, 4200, null, null, null, null, null));
        this.save(pet, user, LogType.SYMPTOM, LogDetail.VOMITING, 580, new LogValues(null, null, null, null, null, null, Wellbeing.QUIETER, null, null, "twice"));
        this.save(pet, user, LogType.MEDICINE, null, 600, new LogValues(null, null, null, null, null, null, null, "Drontal", "1 tablet", null));
        this.save(pet, user, LogType.NOTE, null, 620, new LogValues(null, null, null, null, null, null, null, null, null, "all fine"));
        this.entityManager.flush();
        this.entityManager.clear();

        List<PetLogDto> served = this.repository.findAllByPetIdAndLocalDateOrderByMinuteOfDayAsc(pet, DAY).stream().map(PetLogDto::of).toList();

        JsonNode actual = this.mapper.valueToTree(served);
        actual.forEach(node -> GENERATED_FIELDS.forEach(((ObjectNode) node)::remove));
        actual.forEach(node -> node.path("foods").forEach(foodNode -> ((ObjectNode) foodNode).remove("petFoodId")));
        assertThat(actual).isEqualTo(this.mapper.readTree(this.fixture()));
    }

    private void save(UUID pet, UUID user, LogType type, LogDetail detail, int minute, LogValues values) {
        LoggedAt loggedAt = new LoggedAt(OffsetDateTime.parse(DAY + "T10:00+02:00"), 120);
        this.repository.save(new PetLog(this.petRepository.getReferenceById(pet), UUID.randomUUID(), type, user, detail, values, DAY, minute, loggedAt));
    }

    private InputStream fixture() {
        return this.getClass().getResourceAsStream("/contracts/pet-log-api-view.json");
    }
}
