package com.picvi.pet_companion_service.petlog.dto;

import java.util.List;

public record PetLogHistoryResponse(List<PetLogDto> items, String nextCursor) {
}
