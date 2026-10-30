package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.common.TextCursors;
import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.exception.nutrition.AllergyMatchRequired;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodProduct;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher;
import com.picvi.pet_companion_service.nutrition.domain.FoodNameNormaliser;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodProductRules;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.dto.AddFoodProductRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductDto;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductsResponse;
import com.picvi.pet_companion_service.nutrition.dto.IngredientDto;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.IngredientSourceRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FoodCatalogueService {
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final FoodProductRepository productRepository;
    private final FoodProductIngredientRepository ingredientRepository;
    private final IngredientSourceRepository sourceRepository;
    private final FoodProductRules rules;
    private final PetService petService;
    private final ConditionApi conditionApi;
    private final AllergyMatcher allergyMatcher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FoodProductsResponse search(UUID userId, String query, FoodKind kind, FoodForm form, LifeStage lifeStage,
                                      String cursor, Integer limit) {
        int pageSize = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        int offset = TextCursors.parseOffset(cursor);
        String normalised = FoodNameNormaliser.normalise(query);
        String likePatterns = normalised.isEmpty() ? "" : Arrays.stream(normalised.split(" "))
                .map(word -> "%" + word + "%")
                .collect(Collectors.joining(","));

        List<FoodProduct> found = this.productRepository.search(userId, kind == null ? null : kind.name(),
                form == null ? null : form.name(), lifeStage == null ? null : lifeStage.name(), normalised,
                likePatterns, pageSize + 1, offset);
        boolean hasMore = found.size() > pageSize;
        List<FoodProduct> page = hasMore ? found.subList(0, pageSize) : found;
        return new FoodProductsResponse(this.toDtos(page), hasMore ? TextCursors.formatOffset(offset + pageSize) : null);
    }

    public FoodProductDto addOwnProduct(UUID userId, AddFoodProductRequest request) {
        List<IngredientDto> ingredients = request.ingredients() == null ? List.of() : request.ingredients();
        ProteinStatus proteinStatus = this.rules.validate(
                ingredients.stream().map(i -> new FoodProductRules.IngredientInput(i.code(), i.role())).toList(),
                request.notSure());
        this.requireActiveCodes(ingredients);
        List<AllergyMatch> matches = this.allergyMatches(userId, request, ingredients);

        String brand = blankToNull(request.brand());
        String name = request.productName().trim();
        String flavour = blankToNull(request.flavour());
        FoodProduct product = FoodProduct.ownerAdded(userId, brand, name, flavour, request.form(), request.kind(),
                request.lifeStage() == null ? LifeStage.ALL : request.lifeStage(),
                request.complete() == null ? Completeness.UNKNOWN : request.complete(),
                proteinStatus, FoodNameNormaliser.normalise(brand, name, flavour), this.clock.instant());
        FoodProduct saved = this.productRepository.save(product);

        List<FoodProductIngredient> rows = ingredients.stream()
                .map(i -> new FoodProductIngredient(saved.getId(), i.code().name(), i.role(), i.hydrolysed()))
                .toList();
        this.ingredientRepository.saveAll(rows);
        return FoodProductDto.of(saved, rows.stream().map(IngredientDto::of).toList(), matches);
    }

    /** Checked against the pet's allergens when the request says which pet the food is for; saving needs a confirmation if any match. */
    private List<AllergyMatch> allergyMatches(UUID userId, AddFoodProductRequest request, List<IngredientDto> ingredients) {
        if (request.petId() == null) {
            return List.of();
        }
        this.petService.getOwnedPetProfile(userId, request.petId());
        List<AllergyMatch> matches = this.allergyMatcher.match(
                ingredients.stream().map(i -> new AllergyMatcher.Ingredient(i.code(), i.hydrolysed())).toList(),
                this.conditionApi.allergens(request.petId(), LocalDate.now(this.clock)));
        if (!matches.isEmpty() && !request.confirmed()) {
            throw new AllergyMatchRequired(matches);
        }
        return matches;
    }

    private void requireActiveCodes(List<IngredientDto> ingredients) {
        List<String> codes = ingredients.stream().map(i -> i.code().name()).toList();
        this.sourceRepository.findAllById(codes).stream()
                .filter(source -> !"ACTIVE".equals(source.getStatus()))
                .findFirst()
                .ifPresent(source -> {
                    throw new InvalidFoodProduct(source.getCode() + " is no longer available");
                });
    }

    private List<FoodProductDto> toDtos(List<FoodProduct> products) {
        Map<UUID, List<IngredientDto>> byProduct = this.ingredientRepository
                .findAllByProductIdIn(products.stream().map(FoodProduct::getId).toList()).stream()
                .collect(Collectors.groupingBy(FoodProductIngredient::getProductId,
                        Collectors.mapping(IngredientDto::of, Collectors.toList())));
        return products.stream()
                .map(product -> FoodProductDto.of(product, byProduct.getOrDefault(product.getId(), List.of())))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
