package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
@Slf4j
@Tag(name = "Upload", description = "Upload file ảnh")
public class FileController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080/api/files}")
    private String baseUrl;

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("EMPTY_FILE", "File không được để trống"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_FILE_TYPE", "Chỉ chấp nhận file ảnh"));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FILE_TOO_LARGE", "File tối đa 10MB"));
        }

        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);

        String url = baseUrl + "/" + filename;
        log.info("Uploaded file: {}", filename);

        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url, "filename", filename)));
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : ".jpg";
    }
}
