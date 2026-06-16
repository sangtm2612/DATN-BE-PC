package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();
    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.sortOrder")
    List<Category> findRootCategories();
}
