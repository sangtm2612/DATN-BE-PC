package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Tag;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.TagRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Tag sản phẩm")
public class TagController {

    private final TagRepository tagRepo;

    @GetMapping
    @Operation(summary = "Danh sách tag, ho tro tim kiem qua ?search=")
    public ResponseEntity<ApiResponse<List<Tag>>> getAll(
            @RequestParam(required = false) String search) {
        List<Tag> tags = (search == null || search.isBlank())
            ? tagRepo.findAll()
            : tagRepo.findByNameContainingIgnoreCase(search);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo tag mới (Admin/Staff)")
    public ResponseEntity<ApiResponse<Tag>> create(@RequestBody(required = false) Tag req) {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw AppException.badRequest("MISSING_NAME", "Vui lòng nhập tên tag");
        }
        String slug = generateSlug(req.getName());
        Tag tag = Tag.builder().name(req.getName()).slug(slug).build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(tagRepo.save(tag), "Tạo tag thành công"));
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
            .replaceAll("[àáâãäå]", "a").replaceAll("[èéêë]", "e")
            .replaceAll("[ìíîï]", "i").replaceAll("[òóôõö]", "o")
            .replaceAll("[ùúûü]", "u").replaceAll("[ý]", "y")
            .replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-")
            .replaceAll("-+", "-").trim();
        String base = slug;
        int i = 1;
        while (tagRepo.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
