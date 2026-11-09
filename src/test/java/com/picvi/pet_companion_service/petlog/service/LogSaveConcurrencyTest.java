package com.picvi.pet_companion_service.petlog.service;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.NewLog;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=4")
class LogSaveConcurrencyTest extends PostgresTest {
    private static final int PARALLEL_SAVES = 12;
    private static final int SECONDS_TO_FINISH = 5;

    @Autowired
    private PetLogService petLogService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Clock clock;

    @Test
    void manyParallelSavesFinishWithASmallConnectionPool() throws Exception {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        UUID owner = seed.user();
        UUID pet = seed.pet(owner);
        LocalDate today = LocalDate.now(this.clock);

        List<Callable<Boolean>> saves = new ArrayList<>();
        for (int i = 0; i < PARALLEL_SAVES; i++) {
            NewLog log = i % 2 == 0 ? bloodyToilet(i) : weight(i);
            saves.add(() -> this.petLogService.log(owner, pet, log, today).created());
        }

        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_SAVES);
        List<Future<Boolean>> results = pool.invokeAll(saves, SECONDS_TO_FINISH, TimeUnit.SECONDS);
        pool.shutdown();

        for (Future<Boolean> result : results) {
            assertThat(result.isCancelled()).isFalse();
            assertThat(result.get()).isTrue();
        }
    }

    private NewLog bloodyToilet(int minute) {
        LogValues values = new LogValues(null, 6, Set.of(LogFlag.BLOOD), null, null, null, null, null, null, null);
        return new NewLog(UUID.randomUUID(), LogType.TOILET, LogDetail.POO, values, LocalDate.now(this.clock), minute, 120, null);
    }

    private NewLog weight(int minute) {
        LogValues values = new LogValues(null, null, null, null, 2300, null, null, null, null, null);
        return new NewLog(UUID.randomUUID(), LogType.WEIGHT, null, values, LocalDate.now(this.clock), minute, 120, null);
    }
}
