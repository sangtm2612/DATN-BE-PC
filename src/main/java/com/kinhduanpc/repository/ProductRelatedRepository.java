package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ProductRelated;
import com.kinhduanpc.entity.ProductRelatedId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRelatedRepository extends JpaRepository<ProductRelated, ProductRelatedId> {
    List<ProductRelated> findByProductIdOrderBySortOrderAsc(Long productId);
    void deleteByProductId(Long productId);
}
