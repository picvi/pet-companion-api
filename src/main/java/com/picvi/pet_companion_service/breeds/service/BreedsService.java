package com.picvi.pet_companion_service.breeds.service;

import com.picvi.pet_companion_service.breeds.dto.BreedDTO;
import com.picvi.pet_companion_service.breeds.entity.Breed;
import com.picvi.pet_companion_service.breeds.repository.BreedsRepository;
import com.picvi.pet_companion_service.exception.breed.BreedNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BreedsService {
    private final BreedsRepository breedsRepository;

    public List<BreedDTO>  getAllBreeds() {
        return this.breedsRepository.findAllBy();
    }

    public Breed getBreedById(BigInteger id) {
        return this.breedsRepository.findBreedById(id).orElseThrow(BreedNotFound::new);
    }
}
