package com.kinhduanpc.service;

import com.kinhduanpc.dto.blog.BlogCategoryDTO;
import com.kinhduanpc.dto.blog.BlogPostRequest;
import com.kinhduanpc.dto.blog.BlogPostResponse;
import com.kinhduanpc.entity.BlogCategory;
import com.kinhduanpc.entity.BlogPost;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BlogCategoryRepository;
import com.kinhduanpc.repository.BlogPostRepository;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogPostRepo;
    private final BlogCategoryRepository blogCategoryRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<BlogCategoryDTO> getAllCategories() {
        return blogCategoryRepo.findAll().stream()
                .map(this::toCategoryDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BlogPostResponse> getAllPosts(Long categoryId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> result;
        
        if (keyword != null && !keyword.isBlank()) {
            result = blogPostRepo.searchByKeyword(keyword, pageable);
        } else if (categoryId != null) {
            result = blogPostRepo.findByBlogCategoryIdAndIsPublishedTrue(categoryId, pageable);
        } else {
            result = blogPostRepo.findByIsPublishedTrueOrderByPublishedAtDesc(pageable);
        }
        
        return result.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getBySlug(String slug) {
        BlogPost post = blogPostRepo.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> AppException.notFound("Bài viết"));
        
        // Increment view count
        blogPostRepo.incrementViewCount(post.getId());
        
        return toResponse(post);
    }

    public BlogPostResponse createPost(BlogPostRequest request, Long authorId) {
        BlogCategory category = blogCategoryRepo.findById(request.getBlogCategoryId())
                .orElseThrow(() -> AppException.notFound("Danh mục blog"));
        
        User author = userRepo.findById(authorId)
                .orElseThrow(() -> AppException.notFound("Tác giả"));
        
        String slug = generateUniqueSlug(request.getTitle());
        
        BlogPost post = BlogPost.builder()
                .title(request.getTitle())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .thumbnailUrl(request.getThumbnailUrl())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .publishedAt(request.getPublishedAt())
                .metaTitle(request.getMetaTitle())
                .metaDesc(request.getMetaDesc())
                .blogCategory(category)
                .author(author)
                .viewCount(0)
                .mentionedProducts(new ArrayList<>())
                .build();
        
        // Set mentioned products if provided
        if (request.getMentionedProductIds() != null && !request.getMentionedProductIds().isEmpty()) {
            List<Product> products = productRepo.findAllById(request.getMentionedProductIds());
            post.setMentionedProducts(products);
        }
        
        post = blogPostRepo.save(post);
        return toResponse(post);
    }

    public BlogPostResponse updatePost(Long id, BlogPostRequest request) {
        BlogPost post = blogPostRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Bài viết"));
        
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setExcerpt(request.getExcerpt());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setIsPublished(request.getIsPublished());
        post.setMetaTitle(request.getMetaTitle());
        post.setMetaDesc(request.getMetaDesc());
        
        if (request.getBlogCategoryId() != null) {
            BlogCategory category = blogCategoryRepo.findById(request.getBlogCategoryId())
                    .orElseThrow(() -> AppException.notFound("Danh mục blog"));
            post.setBlogCategory(category);
        }
        
        // Update mentioned products if provided
        if (request.getMentionedProductIds() != null) {
            if (request.getMentionedProductIds().isEmpty()) {
                post.setMentionedProducts(new ArrayList<>());
            } else {
                List<Product> products = productRepo.findAllById(request.getMentionedProductIds());
                post.setMentionedProducts(products);
            }
        }
        
        // Update slug if title changed
        if (!post.getTitle().equals(request.getTitle())) {
            String newSlug = generateUniqueSlug(request.getTitle());
            post.setSlug(newSlug);
        }
        
        post = blogPostRepo.save(post);
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public List<Long> getMentionedProductIds(Long id) {
        BlogPost post = blogPostRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Bài viết"));
        
        return post.getMentionedProducts().stream()
                .map(Product::getId)
                .toList();
    }

    public void setMentionedProducts(Long id, List<Long> productIds) {
        BlogPost post = blogPostRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Bài viết"));
        
        if (productIds == null || productIds.isEmpty()) {
            post.setMentionedProducts(new ArrayList<>());
        } else {
            List<Product> products = productRepo.findAllById(productIds);
            post.setMentionedProducts(products);
        }
        
        blogPostRepo.save(post);
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = generateSlug(title);
        String slug = baseSlug;
        int counter = 1;
        
        while (blogPostRepo.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        
        return slug;
    }

    private String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(normalized).replaceAll("");
        
        return withoutAccents.toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private BlogCategoryDTO toCategoryDTO(BlogCategory category) {
        return BlogCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .sortOrder(category.getSortOrder())
                .build();
    }

    private BlogPostResponse toResponse(BlogPost post) {
        return BlogPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .thumbnailUrl(post.getThumbnailUrl())
                .viewCount(post.getViewCount())
                .isPublished(post.getIsPublished())
                .publishedAt(post.getPublishedAt())
                .metaTitle(post.getMetaTitle())
                .metaDesc(post.getMetaDesc())
                .blogCategory(post.getBlogCategory())
                .author(post.getAuthor() != null
                        ? new BlogPostResponse.AuthorSummary(
                                post.getAuthor().getId(),
                                post.getAuthor().getFullName())
                        : null)
                .mentionedProducts(post.getMentionedProducts())
                .build();
    }
}
