package com.picvi.pet_companion_service.breeds.repository;

import com.picvi.pet_companion_service.breeds.dto.BreedDTO;
import com.picvi.pet_companion_service.breeds.entity.Breed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface BreedsRepository extends JpaRepository<Breed, BigInteger> {
    List<BreedDTO> findAllBy();

    Optional<Breed> findBreedById(BigInteger id);
}
