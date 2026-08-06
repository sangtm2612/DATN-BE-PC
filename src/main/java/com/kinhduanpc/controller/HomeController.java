package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.HomeDataDTO;
import com.kinhduanpc.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Dữ liệu trang chủ")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    @Operation(summary = "Lấy toàn bộ data trang chủ")
    public ResponseEntity<ApiResponse<HomeDataDTO>> getHomeData() {
        return ResponseEntity.ok(ApiResponse.success(homeService.getHomeData()));
    }
}
