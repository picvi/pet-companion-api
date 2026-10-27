package com.picvi.pet_companion_service.auth.repository;

import com.picvi.pet_companion_service.auth.entity.EmailVerificationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    /** Locked, so parallel guesses queue up and each one is counted before the next is checked. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationCode> findByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

    @Modifying
    @Query("delete from EmailVerificationCode c where c.expiresAt < :now")
    int deleteExpired(Instant now);
}
