package com.picvi.pet_companion_service.medication.dto;

import java.time.LocalDate;
import java.util.List;

public record CoursesResponse(LocalDate date, List<CourseDto> courses) {
}
