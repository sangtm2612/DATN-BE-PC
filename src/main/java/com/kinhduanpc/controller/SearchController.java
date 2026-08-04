package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.repository.SearchHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Goi y tim kiem tu lich su")
public class SearchController {

    private final SearchHistoryRepository searchHistoryRepo;

    @GetMapping("/suggestions")
    @Operation(summary = "Goi y tu khoa tu lich su tim kiem (toi da 10, moi nhat truoc)")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        var history = userId != null
            ? searchHistoryRepo.findTop10ByUserIdOrderBySearchedAtDesc(userId)
            : (sessionId != null ? searchHistoryRepo.findTop10BySessionIdOrderBySearchedAtDesc(sessionId) : List.<com.kinhduanpc.entity.SearchHistory>of());

        // Dedupe tu khoa (giu lan gan nhat), gioi han 10
        List<String> keywords = new LinkedHashSet<>(history.stream().map(h -> h.getKeyword()).toList())
            .stream().limit(10).toList();

        return ResponseEntity.ok(ApiResponse.success(keywords));
    }
}
