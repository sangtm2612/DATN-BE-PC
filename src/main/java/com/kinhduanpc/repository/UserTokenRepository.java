package com.kinhduanpc.repository;

import com.kinhduanpc.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByToken(String token);
    Optional<UserToken> findByUserIdAndTokenType(Long userId, String tokenType);

    @Modifying
    @Query("DELETE FROM UserToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserToken t WHERE t.user.id = :userId AND t.tokenType = :type")
    void deleteByUserIdAndType(Long userId, String type);
}
