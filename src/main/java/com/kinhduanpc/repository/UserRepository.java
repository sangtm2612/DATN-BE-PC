package com.kinhduanpc.repository;

import com.kinhduanpc.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    java.util.List<User> findByRole(User.UserRole role);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT u FROM User u WHERE u.email = :credential OR u.phone = :credential")
    Optional<User> findByEmailOrPhone(@Param("credential") String credential);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.id = :id")
    void incrementLoginAttempts(@Param("id") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0, u.lockedUntil = null WHERE u.id = :id")
    void resetLoginAttempts(@Param("id") Long userId);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<User> findAllByRoleOrderByCreatedAtDesc(User.UserRole role, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR u.phone           LIKE CONCAT('%', :keyword, '%')
        ORDER BY u.createdAt DESC
        """)
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.role = :role
          AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR u.phone        LIKE CONCAT('%', :keyword, '%'))
        ORDER BY u.createdAt DESC
        """)
    Page<User> searchUsersByRole(
        @Param("role") User.UserRole role,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("""
        SELECT u FROM User u
        WHERE u.dateOfBirth IS NOT NULL
          AND EXTRACT(MONTH FROM u.dateOfBirth) = EXTRACT(MONTH FROM CURRENT_DATE)
          AND EXTRACT(DAY FROM u.dateOfBirth) = EXTRACT(DAY FROM CURRENT_DATE)
          AND u.status = 'active'
        """)
    List<User> findUsersWithBirthdayToday();
}
