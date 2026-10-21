package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.exception.nutrition.FoodProductNotFound;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodProduct;
import com.picvi.pet_companion_service.nutrition.domain.ChangeReason;
import com.picvi.pet_companion_service.nutrition.domain.FoodNameNormaliser;
import com.picvi.pet_companion_service.nutrition.domain.FoodProductRules;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.dto.ApproveFoodProductRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductDto;
import com.picvi.pet_companion_service.nutrition.dto.IngredientDto;
import com.picvi.pet_companion_service.nutrition.dto.PendingFoodGroupDto;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductChange;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductChangeRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** The admin's side of owner-added foods: see what is waiting, make one public (merging the same food typed by other owners), or reject it. */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminFoodService {
    private final FoodProductRepository productRepository;
    private final FoodProductIngredientRepository ingredientRepository;
    private final FoodProductChangeRepository changeRepository;
    private final FoodProductRules rules;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PendingFoodGroupDto> pending() {
        List<FoodProduct> waiting = this.productRepository.findAllByStatusOrderByCreatedAtAsc(ProductStatus.OWNER_ADDED);
        Map<UUID, List<IngredientDto>> ingredients = this.ingredientsOf(waiting);
        Map<String, List<FoodProduct>> groups = waiting.stream()
                .collect(Collectors.groupingBy(FoodProduct::getNormalisedName, LinkedHashMap::new, Collectors.toList()));
        return groups.entrySet().stream()
                .map(group -> new PendingFoodGroupDto(group.getKey(),
                        (int) group.getValue().stream().map(FoodProduct::getCreatedBy).distinct().count(),
                        group.getValue().stream().map(p -> FoodProductDto.of(p, ingredients.getOrDefault(p.getId(), List.of()))).toList()))
                .toList();
    }

    public FoodProductDto approve(UUID adminId, UUID productId, ApproveFoodProductRequest request) {
        FoodProduct target = this.waiting(productId);
        List<IngredientDto> ingredients = request.ingredients() == null ? List.of() : request.ingredients();
        ProteinStatus proteinStatus = this.rules.validate(
                ingredients.stream().map(i -> new FoodProductRules.IngredientInput(i.code(), i.role())).toList(), request.notSure());

        LocalDate today = LocalDate.now(this.clock);
        Instant now = this.clock.instant();
        OffsetDateTime nowAt = OffsetDateTime.now(this.clock);
        List<FoodProduct> copies = this.productRepository.findAllByStatusAndNormalisedName(ProductStatus.OWNER_ADDED, target.getNormalisedName())
                .stream().filter(copy -> !copy.getId().equals(target.getId())).toList();

        Map<String, Object> before = this.snapshot(target, this.ingredientDtos(target.getId()));
        String brand = blankToNull(request.brand());
        String name = request.productName().trim();
        String flavour = blankToNull(request.flavour());
        target.approve(brand, blankToNull(request.productLine()), name, flavour, request.form(), request.kind(),
                request.lifeStage() == null ? target.getLifeStage() : request.lifeStage(),
                request.breedSize() == null ? target.getBreedSize() : request.breedSize(),
                request.complete() == null ? target.getComplete() : request.complete(), proteinStatus,
                FoodNameNormaliser.normalise(brand, name, flavour), blankToNull(request.sourceUrl()), adminId, today, now);
        this.productRepository.saveAndFlush(target);

        this.ingredientRepository.deleteAllByProductId(target.getId());
        this.ingredientRepository.flush();
        List<FoodProductIngredient> rows = ingredients.stream()
                .map(i -> new FoodProductIngredient(target.getId(), i.code().name(), i.role(), i.hydrolysed()))
                .toList();
        this.ingredientRepository.saveAll(rows);
        List<IngredientDto> after = rows.stream().map(IngredientDto::of).toList();
        this.changeRepository.save(new FoodProductChange(target.getId(), adminId, nowAt, ChangeReason.CORRECTION, before, this.snapshot(target, after)));

        if (!copies.isEmpty()) {
            for (FoodProduct copy : copies) {
                Map<String, Object> copyBefore = this.snapshot(copy, this.ingredientDtos(copy.getId()));
                copy.mergeInto(target.getId(), adminId, today, now);
                this.changeRepository.save(new FoodProductChange(copy.getId(), adminId, nowAt, ChangeReason.MERGE, copyBefore,
                        Map.of("status", ProductStatus.MERGED.name(), "mergedIntoId", target.getId().toString())));
            }
            this.productRepository.saveAll(copies);
            this.productRepository.repointPetFoods(target.getId(), copies.stream().map(FoodProduct::getId).toList());
        }
        return FoodProductDto.of(target, after);
    }

    public void reject(UUID adminId, UUID productId) {
        FoodProduct product = this.waiting(productId);
        product.reject(adminId, LocalDate.now(this.clock), this.clock.instant());
        this.productRepository.save(product);
    }

    private FoodProduct waiting(UUID productId) {
        FoodProduct product = this.productRepository.findById(productId).orElseThrow(FoodProductNotFound::new);
        if (product.getStatus() != ProductStatus.OWNER_ADDED) {
            throw new InvalidFoodProduct("Only an owner-added product waiting for review can be approved or rejected");
        }
        return product;
    }

    private List<IngredientDto> ingredientDtos(UUID productId) {
        return this.ingredientRepository.findAllByProductIdIn(List.of(productId)).stream().map(IngredientDto::of).toList();
    }

    private Map<UUID, List<IngredientDto>> ingredientsOf(List<FoodProduct> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        return this.ingredientRepository.findAllByProductIdIn(products.stream().map(FoodProduct::getId).toList()).stream()
                .collect(Collectors.groupingBy(FoodProductIngredient::getProductId, Collectors.mapping(IngredientDto::of, Collectors.toList())));
    }

    private Map<String, Object> snapshot(FoodProduct product, List<IngredientDto> ingredients) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("brand", product.getBrand());
        values.put("productLine", product.getProductLine());
        values.put("productName", product.getProductName());
        values.put("flavour", product.getFlavour());
        values.put("form", product.getForm().name());
        values.put("kind", product.getKindHint().name());
        values.put("lifeStage", product.getLifeStage().name());
        values.put("complete", product.getComplete().name());
        values.put("proteinStatus", product.getProteinStatus().name());
        values.put("status", product.getStatus().name());
        List<String> codes = new ArrayList<>();
        ingredients.forEach(i -> codes.add(i.code() + ":" + i.role() + (i.hydrolysed() ? ":hydrolysed" : "")));
        codes.sort(String::compareTo);
        values.put("ingredients", codes);
        return values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
