package com.picvi.pet_companion_service.nutrition.domain;

import static com.picvi.pet_companion_service.nutrition.domain.IngredientKind.CARB;
import static com.picvi.pet_companion_service.nutrition.domain.IngredientKind.PROTEIN;

import lombok.Getter;

/**
 * Mirrors the ingredient_source table; ReferenceCodeCheck fails startup when they differ. A code is never renamed
 * or removed, only deprecated in the table. A parent is the generic term a label may use instead of the animal:
 * a food that names only POULTRY may contain CHICKEN.
 */
public enum IngredientCode {
    POULTRY(PROTEIN, null),
    CHICKEN(PROTEIN, "POULTRY"),
    TURKEY(PROTEIN, "POULTRY"),
    DUCK(PROTEIN, "POULTRY"),
    GOOSE(PROTEIN, "POULTRY"),
    QUAIL(PROTEIN, "POULTRY"),
    PHEASANT(PROTEIN, "POULTRY"),
    GUINEA_FOWL(PROTEIN, "POULTRY"),
    MEAT_UNSPECIFIED(PROTEIN, null),
    BEEF(PROTEIN, "MEAT_UNSPECIFIED"),
    LAMB(PROTEIN, "MEAT_UNSPECIFIED"),
    PORK(PROTEIN, "MEAT_UNSPECIFIED"),
    VENISON(PROTEIN, "MEAT_UNSPECIFIED"),
    RABBIT(PROTEIN, "MEAT_UNSPECIFIED"),
    GOAT(PROTEIN, "MEAT_UNSPECIFIED"),
    WILD_BOAR(PROTEIN, "MEAT_UNSPECIFIED"),
    HORSE(PROTEIN, "MEAT_UNSPECIFIED"),
    KANGAROO(PROTEIN, "MEAT_UNSPECIFIED"),
    FISH(PROTEIN, null),
    SALMON(PROTEIN, "FISH"),
    TROUT(PROTEIN, "FISH"),
    COD(PROTEIN, "FISH"),
    HERRING(PROTEIN, "FISH"),
    SARDINE(PROTEIN, "FISH"),
    TUNA(PROTEIN, "FISH"),
    SHELLFISH(PROTEIN, null),
    EGG(PROTEIN, null),
    DAIRY(PROTEIN, null),
    INSECT(PROTEIN, null),
    RICE(CARB, null),
    WHEAT(CARB, null),
    MAIZE(CARB, null),
    OAT(CARB, null),
    BARLEY(CARB, null),
    SOY(CARB, null),
    PEA(CARB, null),
    POTATO(CARB, null),
    SWEET_POTATO(CARB, null);

    @Getter
    private final IngredientKind kind;
    private final String parentName;

    IngredientCode(IngredientKind kind, String parentName) {
        this.kind = kind;
        this.parentName = parentName;
    }

    public IngredientCode parent() {
        return this.parentName == null ? null : IngredientCode.valueOf(this.parentName);
    }
}
