package com.picvi.pet_companion_service.nutrition.digest;

import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodDigestService {
    private final FoodProductRepository productRepository;
    private final FoodProductIngredientRepository ingredientRepository;
    private final DigestMailer mailer;
    private final FoodDigestFormatter formatter;
    private final Clock clock;

    /**
     * Emails everything owner-added that no digest has listed yet, then marks it. If the send fails nothing is marked, so
     * the next run tries again. Nothing is sent when there is nothing new.
     *
     * @return how many products were listed
     */
    @Transactional
    public int sendPending(List<String> recipients) {
        List<FoodProduct> pending = this.productRepository.findAllByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(ProductStatus.OWNER_ADDED);
        if (pending.isEmpty()) {
            return 0;
        }
        Map<UUID, List<FoodProductIngredient>> ingredients = this.ingredientRepository
                .findAllByProductIdIn(pending.stream().map(FoodProduct::getId).toList()).stream()
                .collect(Collectors.groupingBy(FoodProductIngredient::getProductId));
        List<DigestInput> inputs = pending.stream()
                .map(product -> new DigestInput(product.getId(), product.getCreatedBy(), product.getBrand(), product.getProductName(),
                        product.getFlavour(), product.getNormalisedName(), product.getForm(), product.getComplete(),
                        product.getProteinStatus(),
                        ingredients.getOrDefault(product.getId(), List.of()).stream()
                                .filter(row -> row.getRole() == IngredientRole.MAIN)
                                .map(row -> com.picvi.pet_companion_service.nutrition.domain.IngredientCode.valueOf(row.getSourceCode()))
                                .toList()))
                .toList();
        List<DigestEntry> entries = this.formatter.entries(inputs);

        this.mailer.send(recipients, this.formatter.subject(entries), this.formatter.body(entries));

        OffsetDateTime now = OffsetDateTime.now(this.clock);
        pending.forEach(product -> product.setNotifiedAt(now));
        this.productRepository.saveAll(pending);
        return pending.size();
    }
}
