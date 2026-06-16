package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
    Optional<Cart> findBySessionId(String sessionId);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.expiresAt < :now AND c.user IS NULL")
    void deleteExpiredGuestCarts(LocalDateTime now);
}
