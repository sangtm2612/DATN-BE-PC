package com.kinhduanpc.repository;

import com.kinhduanpc.entity.BlogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {
    Optional<BlogCategory> findBySlug(String slug);
}
