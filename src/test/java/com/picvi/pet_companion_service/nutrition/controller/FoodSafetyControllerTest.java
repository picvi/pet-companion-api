package com.picvi.pet_companion_service.nutrition.controller;

import com.picvi.pet_companion_service.nutrition.domain.ReviewStatus;
import com.picvi.pet_companion_service.nutrition.dto.FoodSafetyResponse;
import com.picvi.pet_companion_service.nutrition.dto.Review;
import com.picvi.pet_companion_service.nutrition.service.FoodSafetyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FoodSafetyControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        FoodSafetyService service = mock(FoodSafetyService.class);
        when(service.catalogue()).thenReturn(new FoodSafetyResponse(new Review(ReviewStatus.PENDING, null, null), List.of()));
        this.mvc = MockMvcBuilders.standaloneSetup(new FoodSafetyController(service)).build();
    }

    @Test
    void returnsTheListWithAnETag() throws Exception {
        this.mvc.perform(get("/reference/food-safety"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.review.status").value("PENDING"));
    }

    @Test
    void answersNotModifiedWhenTheETagStillMatches() throws Exception {
        String etag = this.mvc.perform(get("/reference/food-safety")).andReturn().getResponse().getHeader("ETag");

        this.mvc.perform(get("/reference/food-safety").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }
}
