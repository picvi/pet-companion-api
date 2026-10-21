package com.picvi.pet_companion_service.development.dto;

import java.util.List;

/** Sessions are oldest first, so they can be drawn left to right. */
public record AloneOverviewResponse(String code, int learnedAfterSeconds, int suggestedNextSeconds,
                                    Integer longestCalmSeconds, List<AloneSessionDto> sessions,
                                    ActivityProgressResponse progress) {
}
