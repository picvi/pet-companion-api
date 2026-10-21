package com.picvi.pet_companion_service.brief.domain;

import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ConcernCounter {

    public record Tally(int count, LocalDate firstSeen, LocalDate lastSeen, Map<LocalDate, Integer> daily) {
    }

    /** Only concerns with at least one matching log appear. One log can count towards several concerns. */
    public Map<Concern, Tally> count(List<LogSnapshot> logs) {
        Map<Concern, Map<LocalDate, Integer>> byConcern = new EnumMap<>(Concern.class);
        for (LogSnapshot log : logs) {
            for (Concern concern : Concern.fromLogs()) {
                if (concern.matches(log)) {
                    byConcern.computeIfAbsent(concern, key -> new TreeMap<>()).merge(log.localDate(), 1, Integer::sum);
                }
            }
        }
        Map<Concern, Tally> tallies = new EnumMap<>(Concern.class);
        byConcern.forEach((concern, daily) -> tallies.put(concern, new Tally(
                daily.values().stream().mapToInt(Integer::intValue).sum(),
                ((TreeMap<LocalDate, Integer>) daily).firstKey(),
                ((TreeMap<LocalDate, Integer>) daily).lastKey(),
                new HashMap<>(daily))));
        return tallies;
    }
}
