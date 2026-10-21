package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodProduct;
import com.picvi.pet_companion_service.exception.nutrition.ProteinRequired;
import com.picvi.pet_companion_service.nutrition.domain.ChangeReason;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodProductRules;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.dto.ApproveFoodProductRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductDto;
import com.picvi.pet_companion_service.nutrition.dto.IngredientDto;
import com.picvi.pet_companion_service.nutrition.dto.PendingFoodGroupDto;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductChange;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductChangeRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFoodServiceTest {
    private static final UUID ADMIN = UUID.randomUUID();

    @Mock
    private FoodProductRepository productRepository;
    @Mock
    private FoodProductIngredientRepository ingredientRepository;
    @Mock
    private FoodProductChangeRepository changeRepository;

    private AdminFoodService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T06:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new AdminFoodService(this.productRepository, this.ingredientRepository, this.changeRepository,
                new FoodProductRules(), clock);
    }

    private FoodProduct ownerFood(String name) {
        FoodProduct product = FoodProduct.ownerAdded(UUID.randomUUID(), null, name, null, FoodForm.DRY, FoodKind.MAIN, LifeStage.ALL,
                Completeness.UNKNOWN, ProteinStatus.KNOWN, name.toLowerCase(), Instant.now());
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }

    private static ApproveFoodProductRequest corrected(IngredientDto... ingredients) {
        return new ApproveFoodProductRequest("Hodowca", null, "Karma od hodowcy", null, FoodForm.DRY, FoodKind.MAIN, null, null,
                Completeness.YES, false, List.of(ingredients), null);
    }

    @Test
    void approvingMakesTheFoodPublicAndMergesTheSameFoodTypedByOthers() {
        FoodProduct target = this.ownerFood("Karma od hodowcy");
        FoodProduct copy1 = this.ownerFood("Karma od hodowcy");
        FoodProduct copy2 = this.ownerFood("Karma od hodowcy");
        when(this.productRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(this.productRepository.findAllByStatusAndNormalisedName(ProductStatus.OWNER_ADDED, "karma od hodowcy"))
                .thenReturn(List.of(target, copy1, copy2));
        when(this.ingredientRepository.findAllByProductIdIn(anyCollection())).thenReturn(List.of());

        FoodProductDto dto = this.service.approve(ADMIN, target.getId(), corrected(new IngredientDto(IngredientCode.CHICKEN, IngredientRole.MAIN, false)));

        assertThat(target.getStatus()).isEqualTo(ProductStatus.PUBLIC);
        assertThat(target.getBrand()).isEqualTo("Hodowca");
        assertThat(target.getComplete()).isEqualTo(Completeness.YES);
        assertThat(target.getReviewedBy()).isEqualTo(ADMIN);
        assertThat(target.getReviewedOn()).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(target.getNormalisedName()).isEqualTo("hodowca karma od hodowcy");
        assertThat(dto.ingredients()).extracting(IngredientDto::code).containsExactly(IngredientCode.CHICKEN);
        assertThat(List.of(copy1, copy2)).allSatisfy(copy -> {
            assertThat(copy.getStatus()).isEqualTo(ProductStatus.MERGED);
            assertThat(copy.getMergedIntoId()).isEqualTo(target.getId());
        });
        verify(this.productRepository).repointPetFoods(eq(target.getId()), eq(List.of(copy1.getId(), copy2.getId())));
    }

    @Test
    void everyCorrectionAndMergeIsAudited() {
        FoodProduct target = this.ownerFood("Karma");
        FoodProduct copy = this.ownerFood("Karma");
        when(this.productRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(this.productRepository.findAllByStatusAndNormalisedName(ProductStatus.OWNER_ADDED, "karma")).thenReturn(List.of(target, copy));
        when(this.ingredientRepository.findAllByProductIdIn(anyCollection())).thenReturn(List.of());

        this.service.approve(ADMIN, target.getId(), corrected(new IngredientDto(IngredientCode.BEEF, IngredientRole.MAIN, false)));

        ArgumentCaptor<FoodProductChange> changes = ArgumentCaptor.forClass(FoodProductChange.class);
        verify(this.changeRepository, org.mockito.Mockito.times(2)).save(changes.capture());
        assertThat(changes.getAllValues()).extracting(FoodProductChange::getReason).containsExactly(ChangeReason.CORRECTION, ChangeReason.MERGE);
        FoodProductChange correction = changes.getAllValues().get(0);
        assertThat(correction.getBefore()).containsEntry("status", "OWNER_ADDED").containsEntry("brand", null);
        assertThat(correction.getAfter()).containsEntry("status", "PUBLIC").containsEntry("brand", "Hodowca");
    }

    @Test
    void approvingNeedsAMainProteinOrNotSure() {
        FoodProduct target = this.ownerFood("Karma");
        when(this.productRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> this.service.approve(ADMIN, target.getId(), corrected())).isInstanceOf(ProteinRequired.class);
        assertThat(target.getStatus()).isEqualTo(ProductStatus.OWNER_ADDED);
    }

    @Test
    void onlyAFoodWaitingForReviewCanBeApprovedOrRejected() {
        FoodProduct done = this.ownerFood("Karma");
        done.reject(ADMIN, LocalDate.now(), Instant.now());
        when(this.productRepository.findById(done.getId())).thenReturn(Optional.of(done));

        assertThatThrownBy(() -> this.service.reject(ADMIN, done.getId())).isInstanceOf(InvalidFoodProduct.class);
        assertThatThrownBy(() -> this.service.approve(ADMIN, done.getId(), corrected(new IngredientDto(IngredientCode.BEEF, IngredientRole.MAIN, false))))
                .isInstanceOf(InvalidFoodProduct.class);
    }

    @Test
    void rejectingMarksTheFoodRejected() {
        FoodProduct product = this.ownerFood("Karma");
        when(this.productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        this.service.reject(ADMIN, product.getId());

        assertThat(product.getStatus()).isEqualTo(ProductStatus.REJECTED);
        assertThat(product.getReviewedBy()).isEqualTo(ADMIN);
        verify(this.productRepository, never()).repointPetFoods(any(), any());
    }

    @Test
    void pendingFoodsAreGroupedByNameWithAnOwnerCount() {
        FoodProduct a = this.ownerFood("Karma");
        FoodProduct b = this.ownerFood("Karma");
        FoodProduct other = this.ownerFood("Inna");
        when(this.productRepository.findAllByStatusOrderByCreatedAtAsc(ProductStatus.OWNER_ADDED)).thenReturn(List.of(a, b, other));
        when(this.ingredientRepository.findAllByProductIdIn(anyCollection())).thenReturn(List.of());

        List<PendingFoodGroupDto> groups = this.service.pending();

        assertThat(groups).extracting(PendingFoodGroupDto::normalisedName).containsExactly("karma", "inna");
        assertThat(groups.get(0).ownerCount()).isEqualTo(2);
        assertThat(groups.get(0).products()).hasSize(2);
    }
}
