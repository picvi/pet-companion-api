package com.picvi.pet_companion_service.medication.controller;

import com.picvi.pet_companion_service.exception.GlobalExceptionHandler;
import com.picvi.pet_companion_service.exception.medication.CourseHasDoses;
import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;
import com.picvi.pet_companion_service.medication.dto.CourseDto;
import com.picvi.pet_companion_service.medication.dto.CoursesResponse;
import com.picvi.pet_companion_service.medication.service.MedicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MedicationControllerTest {
    private static final UUID PET = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();
    private static final String COURSE_JSON = """
            {"clientId":"%s","date":"2026-10-18","name":"Amoxicillin","amountHundredths":100,"unit":"TABLET",
             "schedule":"DAILY","times":[480,1080],"withFood":"YES","startsOn":"2026-10-18","startMinute":1080}
            """.formatted(UUID.randomUUID());
    private static final String UPDATE_JSON = """
            {"date":"2026-10-18","name":"Amoxicillin","amountHundredths":100,"unit":"TABLET",
             "schedule":"DAILY","times":[480],"withFood":"YES","startsOn":"2026-10-18","startMinute":1080}
            """;

    private MedicationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        this.service = mock(MedicationService.class);
        this.mvc = MockMvcBuilders.standaloneSetup(new MedicationController(this.service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void theListComesWithAnETagAndAnswersNotModifiedWhenItStillMatches() throws Exception {
        when(this.service.list(any(), eq(PET), any())).thenReturn(new CoursesResponse(LocalDate.of(2026, 10, 18), List.of(dto())));
        String etag = this.mvc.perform(get("/pets/{petId}/medications", PET).param("date", "2026-10-18"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn().getResponse().getHeader("ETag");

        this.mvc.perform(get("/pets/{petId}/medications", PET).param("date", "2026-10-18").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void aNewCourseIsCreated() throws Exception {
        when(this.service.create(any(), eq(PET), any())).thenReturn(new MedicationService.CreateResult(dto(), true));

        this.mvc.perform(post("/pets/{petId}/medications", PET).contentType(MediaType.APPLICATION_JSON).content(COURSE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void aRetriedCreateIsOk() throws Exception {
        when(this.service.create(any(), eq(PET), any())).thenReturn(new MedicationService.CreateResult(dto(), false));

        this.mvc.perform(post("/pets/{petId}/medications", PET).contentType(MediaType.APPLICATION_JSON).content(COURSE_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void anUpdateNeedsAnIfMatchVersion() throws Exception {
        this.mvc.perform(put("/pets/{petId}/medications/{id}", PET, COURSE).contentType(MediaType.APPLICATION_JSON).content(UPDATE_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theIfMatchVersionIsPassedOn() throws Exception {
        when(this.service.update(any(), eq(PET), eq(COURSE), anyLong(), any())).thenReturn(dto());

        this.mvc.perform(put("/pets/{petId}/medications/{id}", PET, COURSE).header("If-Match", "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_JSON))
                .andExpect(status().isOk());

        verify(this.service).update(any(), eq(PET), eq(COURSE), eq(3L), any());
    }

    @Test
    void aVersionThatIsNotANumberIsABadRequest() throws Exception {
        this.mvc.perform(put("/pets/{petId}/medications/{id}", PET, COURSE).header("If-Match", "abc")
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COURSE"));
    }

    @Test
    void deletingACourseWithDosesIsAConflict() throws Exception {
        doThrow(new CourseHasDoses()).when(this.service).delete(any(), eq(PET), eq(COURSE));

        this.mvc.perform(delete("/pets/{petId}/medications/{id}", PET, COURSE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_HAS_DOSES"));
    }

    @Test
    void deletingACourseWithoutDosesIsNoContent() throws Exception {
        this.mvc.perform(delete("/pets/{petId}/medications/{id}", PET, COURSE))
                .andExpect(status().isNoContent());
    }

    private static CourseDto dto() {
        return new CourseDto(COURSE, UUID.randomUUID(), "Amoxicillin", 100, DoseUnit.TABLET, DoseSchedule.DAILY, null, List.of(480),
                WithFood.YES, LocalDate.of(2026, 10, 18), 480, null, null, null, null, true, CourseStatus.ACTIVE, null, null,
                0, null, List.of(), 0);
    }
}
