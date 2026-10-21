package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.ReviewStatus;
import com.picvi.pet_companion_service.nutrition.dto.FoodSafetyItemDto;
import com.picvi.pet_companion_service.nutrition.dto.FoodSafetyResponse;
import com.picvi.pet_companion_service.nutrition.dto.Review;
import com.picvi.pet_companion_service.nutrition.entity.FoodSafetyItem;
import com.picvi.pet_companion_service.nutrition.repository.FoodSafetyItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodSafetyService {
    private final FoodSafetyItemRepository repository;

    private volatile FoodSafetyResponse catalogue;

    public FoodSafetyResponse catalogue() {
        FoodSafetyResponse cached = this.catalogue;
        if (cached == null) {
            cached = this.load();
            this.catalogue = cached;
        }
        return cached;
    }

    private FoodSafetyResponse load() {
        List<FoodSafetyItem> items = this.repository.findAllByOrderByCodeAsc();
        return new FoodSafetyResponse(reviewOf(items), items.stream().map(FoodSafetyItemDto::of).toList());
    }

    private static Review reviewOf(List<FoodSafetyItem> items) {
        if (items.isEmpty() || items.stream().anyMatch(item -> !item.isReviewed())) {
            return new Review(ReviewStatus.PENDING, null, null);
        }
        FoodSafetyItem latest = items.stream().max(Comparator.comparing(FoodSafetyItem::getReviewedOn)).orElseThrow();
        return new Review(ReviewStatus.REVIEWED, latest.getReviewedBy(), latest.getReviewedOn());
    }
}
