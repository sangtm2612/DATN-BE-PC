package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ProductStockByStore;
import com.kinhduanpc.entity.ProductStockByStoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductStockByStoreRepository extends JpaRepository<ProductStockByStore, ProductStockByStoreId> {
    List<ProductStockByStore> findByProductId(Long productId);
    List<ProductStockByStore> findByStoreId(Long storeId);
}
