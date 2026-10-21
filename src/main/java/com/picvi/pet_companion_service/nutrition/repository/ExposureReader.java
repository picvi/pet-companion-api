package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.domain.ExposureRow;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reads the analytics view v_pet_ingredient_exposure. */
@Repository
@RequiredArgsConstructor
public class ExposureReader {
    private final JdbcTemplate jdbc;

    /** Ingredients from foods given on some day between {@code from} and {@code to}. */
    public List<ExposureRow> read(UUID petId, LocalDate from, LocalDate to) {
        return this.jdbc.query("""
                        select source_code, food_kind, hydrolysed, from_on, to_on
                        from v_pet_ingredient_exposure
                        where pet_id = ? and from_on <= ? and (to_on is null or to_on > ?)
                        """,
                (rs, row) -> new ExposureRow(IngredientCode.valueOf(rs.getString("source_code")),
                        FoodKind.valueOf(rs.getString("food_kind")), rs.getBoolean("hydrolysed"),
                        rs.getObject("from_on", LocalDate.class), rs.getObject("to_on", LocalDate.class)),
                petId, Date.valueOf(to), Date.valueOf(from));
    }
}
