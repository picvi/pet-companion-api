package com.picvi.pet_companion_service.user.repository;

import com.picvi.pet_companion_service.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsersRepository extends JpaRepository<User, UUID> {
    User findByUsername(String userName);

    Optional<User> findByEmail(String email);

    /** Unverified accounts cannot log in, so they own no pets or data; deleting them frees the username and email. */
    @Modifying
    @Query("delete from User u where u.emailVerified = false and u.created_at < :before")
    int deleteUnverifiedCreatedBefore(Instant before);
}
