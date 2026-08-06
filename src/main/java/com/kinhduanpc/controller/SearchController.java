package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Goi y tim kiem tu lich su")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/suggestions")
    @Operation(summary = "Goi y tu khoa tu lich su tim kiem (toi da 10, moi nhat truoc)")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        List<String> keywords = searchService.getSuggestions(userId, sessionId);
        
        return ResponseEntity.ok(ApiResponse.success(keywords));
    }
}
