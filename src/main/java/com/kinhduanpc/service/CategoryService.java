package com.kinhduanpc.service;

import com.kinhduanpc.dto.category.CategoryRequest;
import com.kinhduanpc.dto.category.CategoryResponse;
import com.kinhduanpc.entity.Category;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepo;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepo.findRootCategories()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getTree() {
        return categoryRepo.findRootCategories()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findBySlug(String slug) {
        Category category = categoryRepo.findBySlug(slug)
            .orElseThrow(() -> AppException.notFound("Danh mục"));
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildren(Long id) {
        if (!categoryRepo.existsById(id)) {
            throw AppException.notFound("Danh mục");
        }
        return categoryRepo.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(id)
            .stream()
            .map(CategoryResponse::fromFlat)
            .toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        String slug = generateSlug(request.getName());
        
        if (categoryRepo.findBySlug(slug).isPresent()) {
            throw AppException.conflict("CATEGORY_EXISTS", "Danh mục với tên này đã tồn tại");
        }

        Category.CategoryBuilder builder = Category.builder()
            .name(request.getName())
            .slug(slug)
            .iconUrl(request.getIconUrl())
            .imageUrl(request.getImageUrl())
            .description(request.getDescription())
            .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
            .isActive(request.getIsActive() != null ? request.getIsActive() : true);

        // Set parent if provided
        if (request.getParentId() != null) {
            Category parent = categoryRepo.findById(request.getParentId())
                .orElseThrow(() -> AppException.notFound("Danh mục cha"));
            builder.parent(parent);
        }

        Category saved = categoryRepo.save(builder.build());
        return CategoryResponse.fromFlat(saved);
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Danh mục"));

        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setIconUrl(request.getIconUrl());
        category.setImageUrl(request.getImageUrl());
        category.setDescription(request.getDescription());
        
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        // Update parent if provided
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw AppException.badRequest("INVALID_PARENT", "Danh mục không thể là cha của chính nó");
            }
            Category parent = categoryRepo.findById(request.getParentId())
                .orElseThrow(() -> AppException.notFound("Danh mục cha"));
            category.setParent(parent);
        }

        Category updated = categoryRepo.save(category);
        return CategoryResponse.from(updated);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
            .replaceAll("[èéẹẻẽêềếệểễ]", "e")
            .replaceAll("[ìíịỉĩ]", "i")
            .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
            .replaceAll("[ùúụủũưừứựửữ]", "u")
            .replaceAll("[ỳýỵỷỹ]", "y")
            .replaceAll("[đ]", "d")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
