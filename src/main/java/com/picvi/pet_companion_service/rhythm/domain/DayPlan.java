package com.picvi.pet_companion_service.rhythm.domain;

import java.util.List;
import java.util.Set;

public record DayPlan(List<DayBlock> blocks,
                      List<Integer> toiletTrips,
                      List<Integer> nightTrips,
                      Set<PlanFlag> flags) {
}
