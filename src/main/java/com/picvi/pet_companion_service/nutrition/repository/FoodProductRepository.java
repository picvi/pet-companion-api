package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodProductRepository extends JpaRepository<FoodProduct, UUID> {

    /**
     * PUBLIC products and the caller's own OWNER_ADDED ones. Every word of the query must appear in the product's
     * search text (accent-free, so "kaczką" finds "kaczka"); results are ranked by trigram similarity.
     * Optional form and life stage filters; a life stage filter also keeps products suited to ALL stages.
     * {@code likePatterns} is a comma-separated list of {@code %word%}; an empty string means no text filter.
     */
    @Query(value = """
            select p.* from food_product p
            where (p.status = 'PUBLIC' or (p.status = 'OWNER_ADDED' and p.created_by = :userId))
              and (cast(:kind as text) is null or p.kind_hint = :kind)
              and (cast(:form as text) is null or p.form = :form)
              and (cast(:lifeStage as text) is null or p.life_stage in (:lifeStage, 'ALL'))
              and (:likePatterns = '' or p.search_text like all (string_to_array(:likePatterns, ',')))
            order by word_similarity(:query, p.search_text) desc, p.brand, p.product_name, p.id
            limit :limit offset :offset
            """, nativeQuery = true)
    List<FoodProduct> search(@Param("userId") UUID userId, @Param("kind") String kind, @Param("form") String form,
                             @Param("lifeStage") String lifeStage, @Param("query") String query,
                             @Param("likePatterns") String likePatterns, @Param("limit") int limit, @Param("offset") int offset);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE food_product SET created_by = NULL WHERE created_by = :userId", nativeQuery = true)
    void clearCreatedBy(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE food_product SET reviewed_by = NULL WHERE reviewed_by = :userId", nativeQuery = true)
    void clearReviewedBy(@Param("userId") UUID userId);

    Optional<FoodProduct> findByIdAndStatus(UUID id, ProductStatus status);

    Optional<FoodProduct> findByIdAndStatusAndCreatedBy(UUID id, ProductStatus status, UUID createdBy);

    /** Owner-added products the admin has not heard about yet. */
    List<FoodProduct> findAllByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(ProductStatus status);

    /** Everything waiting for review, oldest first. */
    List<FoodProduct> findAllByStatusOrderByCreatedAtAsc(ProductStatus status);

    List<FoodProduct> findAllByStatusAndNormalisedName(ProductStatus status, String normalisedName);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "update pet_food set product_id = :target where product_id in (:from)", nativeQuery = true)
    int repointPetFoods(@Param("target") UUID target, @Param("from") java.util.Collection<UUID> from);
}
