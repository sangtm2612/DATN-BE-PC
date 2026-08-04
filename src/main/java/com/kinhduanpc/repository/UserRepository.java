package com.kinhduanpc.repository;

import com.kinhduanpc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    java.util.List<User> findByRole(User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.email = :credential OR u.phone = :credential")
    Optional<User> findByEmailOrPhone(@Param("credential") String credential);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.id = :id")
    void incrementLoginAttempts(@Param("id") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0, u.lockedUntil = null WHERE u.id = :id")
    void resetLoginAttempts(@Param("id") Long userId);
}
