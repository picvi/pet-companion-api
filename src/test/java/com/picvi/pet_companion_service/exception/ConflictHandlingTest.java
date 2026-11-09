package com.picvi.pet_companion_service.exception;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConflictHandlingTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        this.mvc = MockMvcBuilders.standaloneSetup(new StubController()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void aLostOptimisticLockIsAConflictTheAppCanReloadFrom() throws Exception {
        this.mvc.perform(get("/stub/stale"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void aUniqueViolationFromARaceIsAConflict() throws Exception {
        this.mvc.perform(get("/stub/unique"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void anyOtherIntegrityViolationIsOurFaultAndStaysAServerError() throws Exception {
        this.mvc.perform(get("/stub/not-null"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void aMissingIfMatchHeaderIsABadRequestWithOurShape() throws Exception {
        this.mvc.perform(put("/stub/versioned"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"))
                .andExpect(jsonPath("$.message").value("If-Match is required"));
    }

    @RestController
    static class StubController {
        @GetMapping("/stub/stale")
        String stale() {
            throw new ObjectOptimisticLockingFailureException("PetFood", "id", new StaleObjectStateException("PetFood", "id"));
        }

        @GetMapping("/stub/unique")
        String unique() {
            throw new DataIntegrityViolationException("duplicate", new SQLException("duplicate key", "23505"));
        }

        @GetMapping("/stub/not-null")
        String notNull() {
            throw new DataIntegrityViolationException("null value", new SQLException("null value", "23502"));
        }

        @PutMapping("/stub/versioned")
        String versioned(@RequestHeader("If-Match") String ifMatch) {
            return ifMatch;
        }
    }
}
