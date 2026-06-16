package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByIsActiveTrueOrderByProvinceAscNameAsc();
    List<Store> findByProvinceAndIsActiveTrue(String province);
    Optional<Store> findBySlug(String slug);
}
