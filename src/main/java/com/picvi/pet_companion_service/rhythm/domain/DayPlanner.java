package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.petlog.domain.LogType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class DayPlanner {
    static final int MEAL_MINUTES = 15;
    static final int WIND_DOWN_MINUTES = 15;
    static final int EVENING_WIND_DOWN_MINUTES = 30;
    static final int BEFORE_BED_TRIP_MINUTES = 10;
    static final int MINUTES_PER_DAY = 1440;
    static final int LOGGED_MEAL_MATCH_MINUTES = 120;

    public DayPlan plan(DayPlanInput input) {
        List<Integer> meals = input.settings().mealMinutes().stream().sorted().toList();
        DayPlan scheduled = new Run(input, meals, input.events(), List.of(), Set.of()).fromWake();

        return this.latestSleepOrWake(input)
                .filter(anchor -> anchor.minute() < input.bedMinute() - EVENING_WIND_DOWN_MINUTES)
                .map(anchor -> this.replan(input, scheduled, anchor, meals))
                .orElse(scheduled);
    }

    private Optional<LoggedEvent> latestSleepOrWake(DayPlanInput input) {
        return input.logs().stream()
                .filter(log -> log.type() == LogType.SLEEP || log.type() == LogType.WAKE)
                .max(Comparator.comparingInt(LoggedEvent::minute));
    }

    private DayPlan replan(DayPlanInput input, DayPlan scheduled, LoggedEvent anchor, List<Integer> meals) {
        List<DayBlock> kept = scheduled.blocks().stream()
                .filter(block -> block.startMinute() < anchor.minute())
                .map(block -> new DayBlock(this.keptKind(block, anchor), block.startMinute(), Math.min(block.endMinute(), anchor.minute())))
                .toList();
        long mealsAlreadyPlaced = kept.stream().filter(block -> block.kind() == BlockKind.MEAL).count();
        List<Integer> mealsLeft = this.withoutLoggedMeals(meals.subList((int) mealsAlreadyPlaced, meals.size()), input.logs());
        Set<Integer> keptTrips = new TreeSet<>(scheduled.toiletTrips().stream().filter(trip -> trip < anchor.minute()).toList());

        List<FixedEvent> eventsLeft = input.events().stream().filter(event -> event.startMinute() >= anchor.minute()).toList();
        Run run = new Run(input, mealsLeft, eventsLeft, kept, keptTrips);
        return anchor.type() == LogType.SLEEP ? run.fromSleep(anchor.minute()) : run.fromWakeAt(anchor.minute());
    }

    private BlockKind keptKind(DayBlock block, LoggedEvent anchor) {
        boolean stillAwakeAtSleepLog = anchor.type() == LogType.SLEEP && block.kind() == BlockKind.NAP && block.endMinute() >= anchor.minute();
        return stillAwakeAtSleepLog ? BlockKind.PLAY : block.kind();
    }

    private List<Integer> withoutLoggedMeals(List<Integer> scheduledMeals, List<LoggedEvent> logs) {
        List<Integer> remaining = new ArrayList<>(scheduledMeals);
        logs.stream().filter(log -> log.type() == LogType.MEAL).forEach(log -> remaining.stream()
                .min(Comparator.comparingInt(meal -> Math.abs(meal - log.minute())))
                .filter(meal -> Math.abs(meal - log.minute()) <= LOGGED_MEAL_MATCH_MINUTES)
                .ifPresent(remaining::remove));
        return remaining;
    }

    private static final class Run {
        private final DayPlanInput input;
        private final int wakeMinute;
        private final int bedMinute;
        private final int eveningStart;
        private final List<DayBlock> blocks;
        private final TreeSet<Integer> toiletTrips;
        private final LinkedList<Integer> pendingMeals;
        private final LinkedList<FixedEvent> pendingEvents;
        private final Set<PlanFlag> flags = EnumSet.noneOf(PlanFlag.class);

        private Run(DayPlanInput input, List<Integer> pendingMeals, List<FixedEvent> events, List<DayBlock> keptBlocks,
                    Set<Integer> keptTrips) {
            this.input = input;
            this.wakeMinute = input.settings().wakeMinute();
            this.bedMinute = input.settings().bedMinute();
            this.eveningStart = this.bedMinute - EVENING_WIND_DOWN_MINUTES;
            this.pendingMeals = new LinkedList<>(pendingMeals);
            this.pendingEvents = new LinkedList<>(events.stream().sorted(Comparator.comparingInt(FixedEvent::startMinute)).toList());
            this.blocks = new ArrayList<>(keptBlocks);
            this.toiletTrips = new TreeSet<>(keptTrips);
        }

        private DayPlan fromWake() {
            this.addBlock(BlockKind.NIGHT, 0, this.wakeMinute);
            return this.fromWakeAt(this.wakeMinute);
        }

        private DayPlan fromWakeAt(int minute) {
            return this.finish(this.runDay(minute));
        }

        private DayPlan fromSleep(int minute) {
            return this.finish(this.runDay(this.planNap(minute)));
        }

        private int runDay(int start) {
            int cursor = start;
            while (cursor < this.eveningStart) {
                cursor = this.afterAwake(this.planAwake(cursor));
            }
            return this.planLeftoverMeals(cursor);
        }

        private DayPlan finish(int eveningStartMinute) {
            this.addBlock(BlockKind.EVENING_WIND_DOWN, eveningStartMinute, this.bedMinute);
            this.addBlock(BlockKind.NIGHT, this.bedMinute, MINUTES_PER_DAY);
            this.addFlags();

            return new DayPlan(this.mergedBlocks(), this.spacedToiletTrips(), this.nightTrips(), this.flags);
        }

        private List<DayBlock> mergedBlocks() {
            List<DayBlock> merged = new ArrayList<>();
            for (DayBlock block : this.blocks) {
                DayBlock previous = merged.isEmpty() ? null : merged.getLast();
                if (previous != null && previous.kind() == block.kind() && block.kind() != BlockKind.MEAL) {
                    merged.set(merged.size() - 1, new DayBlock(previous.kind(), previous.startMinute(), block.endMinute()));
                } else {
                    merged.add(block);
                }
            }
            return merged;
        }

        private int afterAwake(int awakeEnd) {
            if (awakeEnd >= this.eveningStart) {
                return awakeEnd;
            }
            FixedEvent event = this.pendingEvents.peekFirst();
            return event != null && event.startMinute() <= awakeEnd ? this.planEvent(awakeEnd) : this.planNap(awakeEnd);
        }

        private int planEvent(int cursor) {
            FixedEvent event = this.pendingEvents.removeFirst();
            int start = Math.max(event.startMinute(), cursor);
            int end = Math.min(Math.max(event.endMinute(), start), this.eveningStart);
            this.addBlock(BlockKind.EVENT, start, end);
            this.toiletTrips.add(end);

            int afterEvent = end;
            while (!this.pendingMeals.isEmpty() && this.pendingMeals.getFirst() <= afterEvent) {
                afterEvent = this.planMeal(afterEvent);
            }
            return this.planNap(afterEvent);
        }

        private int planAwake(int start) {
            this.toiletTrips.add(start);
            int cursor = start;

            while (!this.pendingMeals.isEmpty() && this.pendingMeals.getFirst() <= cursor) {
                if (this.pendingMeals.getFirst() < start) {
                    this.flags.add(PlanFlag.MEAL_MOVED_FOR_NAP);
                }
                cursor = this.planMeal(cursor);
            }

            int awakeLimit = this.pendingEvents.isEmpty() ? this.eveningStart : Math.min(this.eveningStart, this.pendingEvents.getFirst().startMinute());
            int awakeEnd = Math.max(Math.min(start + this.input.awakeMinutes(), awakeLimit), cursor);
            while (!this.pendingMeals.isEmpty() && this.pendingMeals.getFirst() <= awakeEnd) {
                int mealStart = this.pendingMeals.getFirst();
                this.addBlock(BlockKind.PLAY, cursor, mealStart);
                cursor = this.planMeal(mealStart);
            }

            int windDownStart = Math.max(cursor, awakeEnd - WIND_DOWN_MINUTES);
            this.addBlock(BlockKind.PLAY, cursor, windDownStart);
            this.addBlock(BlockKind.WIND_DOWN, windDownStart, awakeEnd);
            this.toiletTrips.add(windDownStart);
            return Math.max(awakeEnd, cursor);
        }

        private int planNap(int start) {
            int napLimit = this.pendingEvents.isEmpty() ? this.eveningStart
                    : Math.min(this.eveningStart, this.pendingEvents.getFirst().startMinute() - WIND_DOWN_MINUTES);
            int end = Math.min(start + this.input.rule().napMinutes().standard(), napLimit);
            this.addBlock(BlockKind.NAP, start, end);
            return Math.max(end, start);
        }

        private int planMeal(int start) {
            int end = start + MEAL_MINUTES;
            this.addBlock(BlockKind.MEAL, start, end);
            this.toiletTrips.add(end);
            this.pendingMeals.removeFirst();
            return end;
        }

        private int planLeftoverMeals(int cursor) {
            int end = cursor;
            while (!this.pendingMeals.isEmpty()) {
                end = this.planMeal(end);
            }
            return end;
        }

        private void addBlock(BlockKind kind, int start, int end) {
            if (end > start) {
                this.blocks.add(new DayBlock(kind, start, end));
            }
        }

        private void addFlags() {
            if (this.input.rule().mealSafetyNote()) {
                this.flags.add(PlanFlag.MEAL_SAFETY_NOTE);
            }
            if (this.input.rule().softGroundPlay()) {
                this.flags.add(PlanFlag.SOFT_GROUND_PLAY);
            }
        }

        private List<Integer> spacedToiletTrips() {
            TreeSet<Integer> trips = new TreeSet<>(this.toiletTrips);
            trips.add(this.bedMinute - BEFORE_BED_TRIP_MINUTES);

            List<Integer> spaced = new ArrayList<>();
            for (int trip : trips) {
                this.fillAwakeGap(spaced, trip);
                spaced.add(trip);
            }
            return spaced;
        }

        private void fillAwakeGap(List<Integer> trips, int next) {
            if (trips.isEmpty()) {
                return;
            }
            int interval = this.input.rule().toiletIntervalMinutes();
            int candidate = trips.getLast() + interval;
            while (candidate < next - interval / 2 && this.isAwake(candidate)) {
                trips.add(candidate);
                candidate += interval;
            }
        }

        private boolean isAwake(int minute) {
            return this.blocks.stream()
                    .filter(block -> minute >= block.startMinute() && minute < block.endMinute())
                    .noneMatch(block -> block.kind() == BlockKind.NAP || block.kind() == BlockKind.NIGHT || block.kind() == BlockKind.EVENT);
        }

        private List<Integer> nightTrips() {
            int nightLength = MINUTES_PER_DAY - this.bedMinute + this.wakeMinute;
            int trips = this.input.rule().nightTrips();
            List<Integer> minutes = new ArrayList<>();
            for (int index = 1; index <= trips; index++) {
                minutes.add((this.bedMinute + index * nightLength / (trips + 1)) % MINUTES_PER_DAY);
            }
            return minutes;
        }
    }
}
