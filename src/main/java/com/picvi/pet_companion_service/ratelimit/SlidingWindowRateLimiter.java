package com.picvi.pet_companion_service.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Allows at most {@code limit} hits per key in any {@code window}. In memory, so the limit is per server. */
public class SlidingWindowRateLimiter {
    private static final int PURGE_EVERY = 1_000;

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private int calls;

    public SlidingWindowRateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    /** Records a hit and returns whether it is within the limit. Rejected hits are not recorded. */
    public boolean tryAcquire(String key) {
        long now = this.clock.millis();
        this.purgeSometimes(now);
        Deque<Long> deque = this.hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() <= now - this.windowMillis) {
                deque.pollFirst();
            }
            if (deque.size() >= this.limit) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    /** Seconds until the oldest hit of this key leaves the window and one more is allowed. */
    public long retryAfterSeconds(String key) {
        Deque<Long> deque = this.hits.get(key);
        if (deque == null) {
            return 1;
        }
        synchronized (deque) {
            Long oldest = deque.peekFirst();
            if (oldest == null) {
                return 1;
            }
            long waitMillis = oldest + this.windowMillis - this.clock.millis();
            return Math.max(1, (waitMillis + 999) / 1000);
        }
    }

    private synchronized void purgeSometimes(long now) {
        if (++this.calls % PURGE_EVERY != 0) {
            return;
        }
        this.hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long newest = entry.getValue().peekLast();
                return newest == null || newest <= now - this.windowMillis;
            }
        });
    }
}
