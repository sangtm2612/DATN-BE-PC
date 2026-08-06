package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.TagDTO;
import com.kinhduanpc.dto.TagRequest;
import com.kinhduanpc.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Danh sách tag, hỗ trợ tìm kiếm qua ?search=")
    public ResponseEntity<ApiResponse<List<TagDTO>>> getAll(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(tagService.findAll(search)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo tag mới (Admin/Staff)")
    public ResponseEntity<ApiResponse<TagDTO>> create(
            @Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(tagService.create(request), "Tạo tag thành công"));
    }
}
