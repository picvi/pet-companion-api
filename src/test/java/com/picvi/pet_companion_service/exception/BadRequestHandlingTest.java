package com.picvi.pet_companion_service.exception;

import com.picvi.pet_companion_service.petlog.domain.LogType;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BadRequestHandlingTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        this.mvc = MockMvcBuilders.standaloneSetup(new StubController()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void anInvalidQueryValueIsABadRequest() throws Exception {
        this.mvc.perform(get("/stub").param("type", "NOPE").param("date", "2026-10-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("type has an invalid value"));
    }

    @Test
    void aMissingQueryParameterIsABadRequest() throws Exception {
        this.mvc.perform(get("/stub").param("type", "MEAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("date is required"));
    }

    @Test
    void anUnknownEnumInTheBodyIsABadRequest() throws Exception {
        this.mvc.perform(post("/stub").contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"WOBBLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void brokenJsonIsABadRequest() throws Exception {
        this.mvc.perform(post("/stub").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @RestController
    static class StubController {
        record Body(@NotNull LogType type) {}

        @GetMapping("/stub")
        String read(@RequestParam LogType type, @RequestParam LocalDate date) {
            return "ok";
        }

        @PostMapping("/stub")
        String write(@RequestBody Body body) {
            return "ok";
        }
    }
}
