package com.picvi.pet_companion_service.nutrition.digest;

import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodDigestServiceTest {
    private static final List<String> TO = List.of("admin@example.com");

    @Mock
    private FoodProductRepository productRepository;
    @Mock
    private FoodProductIngredientRepository ingredientRepository;
    @Mock
    private DigestMailer mailer;

    private FoodDigestService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T06:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new FoodDigestService(this.productRepository, this.ingredientRepository, this.mailer, new FoodDigestFormatter(), clock);
    }

    private FoodProduct ownerFood(String name) {
        FoodProduct product = FoodProduct.ownerAdded(UUID.randomUUID(), null, name, null, FoodForm.DRY, FoodKind.MAIN, LifeStage.ALL,
                Completeness.UNKNOWN, ProteinStatus.KNOWN, name.toLowerCase(), Instant.now());
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }

    @Test
    void threeOwnersAddingTheSameFoodAreListedOnceAndMarked() {
        List<FoodProduct> products = List.of(this.ownerFood("Karma od hodowcy"), this.ownerFood("Karma od hodowcy"), this.ownerFood("Karma od hodowcy"));
        when(this.productRepository.findAllByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(ProductStatus.OWNER_ADDED)).thenReturn(products);
        when(this.ingredientRepository.findAllByProductIdIn(any())).thenReturn(List.of(
                new FoodProductIngredient(products.get(0).getId(), "CHICKEN", IngredientRole.MAIN, false)));

        int listed = this.service.sendPending(TO);

        assertThat(listed).isEqualTo(3);
        verify(this.mailer).send(eq(TO), eq("curl up: 1 new owner-added food"), org.mockito.ArgumentMatchers.contains("x3  Karma od hodowcy"));
        assertThat(products).allSatisfy(product -> assertThat(product.getNotifiedAt()).isNotNull());
    }

    @Test
    void nothingNewSendsNothing() {
        when(this.productRepository.findAllByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(ProductStatus.OWNER_ADDED)).thenReturn(List.of());

        assertThat(this.service.sendPending(TO)).isZero();

        verify(this.mailer, never()).send(any(), any(), any());
    }

    @Test
    void aFailedSendLeavesTheProductsUnmarkedForTomorrow() {
        FoodProduct product = this.ownerFood("Karma");
        when(this.productRepository.findAllByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(ProductStatus.OWNER_ADDED)).thenReturn(List.of(product));
        when(this.ingredientRepository.findAllByProductIdIn(any())).thenReturn(List.of());
        doThrow(new IllegalStateException("mail server down")).when(this.mailer).send(any(), any(), any());

        assertThatThrownBy(() -> this.service.sendPending(TO)).isInstanceOf(IllegalStateException.class);

        assertThat(product.getNotifiedAt()).isNull();
        verify(this.productRepository, never()).saveAll(any());
    }
}
