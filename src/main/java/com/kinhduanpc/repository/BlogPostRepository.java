package com.kinhduanpc.repository;

import com.kinhduanpc.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    Optional<BlogPost> findBySlugAndIsPublishedTrue(String slug);
    Page<BlogPost> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
    Page<BlogPost> findByBlogCategoryIdAndIsPublishedTrue(Long categoryId, Pageable pageable);

    @Query("""
        SELECT p FROM BlogPost p WHERE p.isPublished = true
        AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY p.publishedAt DESC
        """)
    Page<BlogPost> searchByKeyword(String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE BlogPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(Long id);
}
