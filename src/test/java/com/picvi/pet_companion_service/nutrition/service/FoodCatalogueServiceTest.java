package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.exception.nutrition.AllergyMatchRequired;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodProduct;
import com.picvi.pet_companion_service.exception.nutrition.ProteinRequired;
import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatchKind;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodProductRules;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.dto.AddFoodProductRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductDto;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductsResponse;
import com.picvi.pet_companion_service.nutrition.dto.IngredientDto;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.entity.IngredientSource;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.IngredientSourceRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodCatalogueServiceTest {
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private FoodProductRepository productRepository;
    @Mock
    private FoodProductIngredientRepository ingredientRepository;
    @Mock
    private IngredientSourceRepository sourceRepository;
    @Mock
    private PetService petService;
    @Mock
    private ConditionApi conditionApi;

    private FoodCatalogueService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new FoodCatalogueService(this.productRepository, this.ingredientRepository, this.sourceRepository,
                new FoodProductRules(), this.petService, this.conditionApi, new AllergyMatcher(), clock);
    }

    private FoodProduct product(String name) {
        FoodProduct product = FoodProduct.ownerAdded(UUID.randomUUID(), "Brit", name, null, FoodForm.DRY, FoodKind.MAIN,
                null, null, ProteinStatus.KNOWN, name.toLowerCase(), Instant.now());
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }

    @Test
    void searchTurnsEachWordIntoAnAccentFreeLikePattern() {
        when(this.productRepository.search(eq(USER), eq(null), anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of());

        this.service.search(USER, "  Brit  PUPPY kaczką ", null, null, null);

        verify(this.productRepository).search(USER, null, "brit puppy kaczka", "%brit%,%puppy%,%kaczka%", 21, 0);
    }

    @Test
    void emptyQueryHasNoTextFilter() {
        when(this.productRepository.search(eq(USER), eq("TREAT"), anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of());

        this.service.search(USER, "", FoodKind.TREAT, null, null);

        verify(this.productRepository).search(USER, "TREAT", "", "", 21, 0);
    }

    @Test
    void searchReturnsNextCursorOnlyWhenThereIsAnotherPage() {
        List<FoodProduct> three = List.of(this.product("a"), this.product("b"), this.product("c"));
        when(this.productRepository.search(any(), any(), anyString(), anyString(), eq(3), eq(0))).thenReturn(three);
        when(this.productRepository.search(any(), any(), anyString(), anyString(), eq(3), eq(2))).thenReturn(three.subList(0, 1));

        FoodProductsResponse first = this.service.search(USER, "x", null, null, 2);
        FoodProductsResponse second = this.service.search(USER, "x", null, first.nextCursor(), 2);

        assertThat(first.items()).hasSize(2);
        assertThat(first.nextCursor()).isEqualTo("2");
        assertThat(second.items()).hasSize(1);
        assertThat(second.nextCursor()).isNull();
    }

    @Test
    void searchAttachesIngredientsToEachProduct() {
        FoodProduct brit = this.product("Puppy Salmon");
        when(this.productRepository.search(any(), any(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of(brit));
        when(this.ingredientRepository.findAllByProductIdIn(any())).thenReturn(List.of(
                new FoodProductIngredient(brit.getId(), "RICE", IngredientRole.OTHER, false),
                new FoodProductIngredient(brit.getId(), "SALMON", IngredientRole.MAIN, false)));

        FoodProductsResponse response = this.service.search(USER, "brit puppy salmon", null, null, null);

        assertThat(response.items().get(0).ingredients())
                .extracting(IngredientDto::code, IngredientDto::role)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(IngredientCode.SALMON, IngredientRole.MAIN),
                        org.assertj.core.groups.Tuple.tuple(IngredientCode.RICE, IngredientRole.OTHER));
    }

    @Test
    void invalidCursorIsRejected() {
        assertThatThrownBy(() -> this.service.search(USER, "", null, "abc", null)).isInstanceOf(InvalidCursor.class);
        assertThatThrownBy(() -> this.service.search(USER, "", null, "-1", null)).isInstanceOf(InvalidCursor.class);
    }

    @Test
    void limitIsClamped() {
        when(this.productRepository.search(any(), any(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of());

        this.service.search(USER, "", null, null, 500);

        verify(this.productRepository).search(USER, null, "", "", 51, 0);
    }

    @Test
    void ownerAddedProductIsVisibleOnlyToItsCreator() {
        when(this.productRepository.save(any(FoodProduct.class))).thenAnswer(call -> {
            FoodProduct saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        AddFoodProductRequest request = new AddFoodProductRequest(null, " Karma od hodowcy ", null, FoodForm.DRY, FoodKind.MAIN,
                null, null, false, List.of(new IngredientDto(IngredientCode.CHICKEN, IngredientRole.MAIN, false)), null, null);

        FoodProductDto dto = this.service.addOwnProduct(USER, request);

        ArgumentCaptor<FoodProduct> saved = ArgumentCaptor.forClass(FoodProduct.class);
        verify(this.productRepository).save(saved.capture());
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(USER);
        assertThat(saved.getValue().getStatus()).isEqualTo(ProductStatus.OWNER_ADDED);
        assertThat(saved.getValue().getNormalisedName()).isEqualTo("karma od hodowcy");
        assertThat(saved.getValue().getProteinStatus()).isEqualTo(ProteinStatus.KNOWN);
        assertThat(dto.status()).isEqualTo(ProductStatus.OWNER_ADDED);
        assertThat(dto.ingredients()).singleElement().satisfies(i -> assertThat(i.code()).isEqualTo(IngredientCode.CHICKEN));
    }

    @Test
    void notSureSavesUnknownProteinWithoutMainRow() {
        when(this.productRepository.save(any(FoodProduct.class))).thenAnswer(call -> call.getArgument(0));
        AddFoodProductRequest request = new AddFoodProductRequest("Brit", "Mystery", null, FoodForm.WET, FoodKind.TREAT,
                null, null, true, List.of(new IngredientDto(IngredientCode.RICE, IngredientRole.OTHER, false)), null, null);

        FoodProductDto dto = this.service.addOwnProduct(USER, request);

        assertThat(dto.proteinStatus()).isEqualTo(ProteinStatus.UNKNOWN);
        assertThat(dto.ingredients()).noneMatch(i -> i.role() == IngredientRole.MAIN);
    }

    @Test
    void noProteinAndNoNotSureIsRejectedBeforeAnythingIsSaved() {
        AddFoodProductRequest request = new AddFoodProductRequest(null, "X", null, FoodForm.DRY, FoodKind.MAIN,
                null, null, false, List.of(), null, null);

        assertThatThrownBy(() -> this.service.addOwnProduct(USER, request)).isInstanceOf(ProteinRequired.class);
        verify(this.productRepository, never()).save(any());
    }

    @Test
    void deprecatedIngredientIsRejected() {
        IngredientSource deprecated = new IngredientSource();
        ReflectionTestUtils.setField(deprecated, "code", "CHICKEN");
        ReflectionTestUtils.setField(deprecated, "status", "DEPRECATED");
        when(this.sourceRepository.findAllById(any())).thenReturn(List.of(deprecated));
        AddFoodProductRequest request = new AddFoodProductRequest(null, "X", null, FoodForm.DRY, FoodKind.MAIN,
                null, null, false, List.of(new IngredientDto(IngredientCode.CHICKEN, IngredientRole.MAIN, false)), null, null);

        assertThatThrownBy(() -> this.service.addOwnProduct(USER, request)).isInstanceOf(InvalidFoodProduct.class);
    }

    @Test
    void aProductForAPetWithAMatchingAllergyNeedsConfirmation() {
        UUID pet = UUID.randomUUID();
        when(this.conditionApi.allergens(eq(pet), any())).thenReturn(List.of(IngredientCode.CHICKEN));
        AddFoodProductRequest request = new AddFoodProductRequest(null, "Treat", null, FoodForm.DRY, FoodKind.TREAT, null, null, false,
                List.of(new IngredientDto(IngredientCode.CHICKEN, IngredientRole.MAIN, false)), pet, null);

        assertThatThrownBy(() -> this.service.addOwnProduct(USER, request))
                .isInstanceOfSatisfying(AllergyMatchRequired.class, e ->
                        assertThat(e.getMatches()).containsExactly(new AllergyMatch(IngredientCode.CHICKEN, AllergyMatchKind.CONTAINS)));
        verify(this.productRepository, never()).save(any());
    }

    @Test
    void aConfirmedMatchIsSavedAndReported() {
        UUID pet = UUID.randomUUID();
        when(this.conditionApi.allergens(eq(pet), any())).thenReturn(List.of(IngredientCode.CHICKEN));
        when(this.productRepository.save(any(FoodProduct.class))).thenAnswer(call -> call.getArgument(0));
        AddFoodProductRequest request = new AddFoodProductRequest(null, "Treat", null, FoodForm.DRY, FoodKind.TREAT, null, null, false,
                List.of(new IngredientDto(IngredientCode.POULTRY, IngredientRole.MAIN, false)), pet, true);

        FoodProductDto dto = this.service.addOwnProduct(USER, request);

        assertThat(dto.allergyMatches()).containsExactly(new AllergyMatch(IngredientCode.CHICKEN, AllergyMatchKind.MAY_CONTAIN));
        verify(this.productRepository).save(any(FoodProduct.class));
    }
}
