package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PetDietLockTest extends PostgresTest {
    @Autowired
    private PetDietLock lock;
    @Autowired
    private FoodTransitionRepository transitionRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void aSecondRequestForTheSamePetWaitsUntilTheFirstIsDone() throws Exception {
        UUID pet = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(this.transactionManager);
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> tx.executeWithoutResult(status -> {
                this.lock.acquire(pet);
                firstHoldsLock.countDown();
                sleep(400);
                order.add("first done");
            }));
            firstHoldsLock.await(5, TimeUnit.SECONDS);
            var second = pool.submit(() -> tx.executeWithoutResult(status -> {
                this.lock.acquire(pet);
                order.add("second got the lock");
            }));
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(order).containsExactly("first done", "second got the lock");
    }

    @Test
    void differentPetsDoNotWaitForEachOther() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(this.transactionManager);
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> tx.executeWithoutResult(status -> {
                this.lock.acquire(UUID.randomUUID());
                firstHoldsLock.countDown();
                await(secondDone);
            }));
            firstHoldsLock.await(5, TimeUnit.SECONDS);
            var second = pool.submit(() -> tx.executeWithoutResult(status -> this.lock.acquire(UUID.randomUUID())));
            second.get(5, TimeUnit.SECONDS);
            secondDone.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void lockingTheOpenChangeOfAPetWithNoneTakesNothing() {
        List<UUID> locked = new TransactionTemplate(this.transactionManager)
                .execute(status -> this.transitionRepository.lockOpen(UUID.randomUUID()));

        assertThat(locked).isEmpty();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
