package com.picvi.pet_companion_service.rhythm.domain;

import java.util.List;

public record DaySettings(int wakeMinute,
                          int bedMinute,
                          List<Integer> mealMinutes,
                          Integer customAwakeMinutes) {
}
