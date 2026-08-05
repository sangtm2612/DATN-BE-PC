package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    boolean existsBySlug(String slug);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isFeatured = true ORDER BY p.soldQty DESC")
    List<Product> findFeaturedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isNew = true ORDER BY p.createdAt DESC")
    List<Product> findNewProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.soldQty DESC")
    List<Product> findBestSellers(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isOnSale = true ORDER BY p.createdAt DESC")
    List<Product> findOnSaleProducts(Pageable pageable);

    @Query("""
        SELECT p FROM Product p WHERE p.isActive = true
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:brandId    IS NULL OR p.brand.id    = :brandId)
        AND (:minPrice   IS NULL OR p.price       >= :minPrice)
        AND (:maxPrice   IS NULL OR p.price       <= :maxPrice)
        """)
    Page<Product> findWithFilters(
        @Param("categoryId") Long categoryId,
        @Param("brandId")    Long brandId,
        @Param("minPrice")   BigDecimal minPrice,
        @Param("maxPrice")   BigDecimal maxPrice,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM Product p WHERE p.isActive = true
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("""
        SELECT p FROM Product p
        WHERE p.isActive = true
          AND p.category.id = :categoryId
          AND p.id <> :excludeId
        ORDER BY p.soldQty DESC
        """)
    List<Product> findRelatedProducts(
        @Param("categoryId") Long categoryId,
        @Param("excludeId")  Long excludeId,
        Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.stockQty <= p.lowStockThreshold AND p.isActive = true")
    List<Product> findLowStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQty <= p.lowStockThreshold AND p.isActive = true")
    long countLowStockProducts();
}
