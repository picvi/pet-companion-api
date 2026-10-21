package com.picvi.pet_companion_service.user.service;

import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;

    public Optional<User> getUserByUUID(UUID uuid) {
        return this.usersRepository.findById(uuid);
    }
}
