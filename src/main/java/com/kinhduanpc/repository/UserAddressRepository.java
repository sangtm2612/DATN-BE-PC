package com.kinhduanpc.repository;

import com.kinhduanpc.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Long userId);
    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefault(Long userId);
}
