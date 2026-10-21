package com.picvi.pet_companion_service.timeline.dto;

import java.util.List;

public record TimelineResponse(List<TimelineItemDto> items, String nextCursor) {
}
