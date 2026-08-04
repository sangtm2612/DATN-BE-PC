package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

    // Whitelist tra ve tu content-type -> extension co dinh, khong tin filename/Content-Type
    // client gui de suy ra phan mo rong (chan svg script injection va extension gia mao).
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp",
        "image/gif", ".gif"
    );

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("EMPTY_FILE", "File không được để trống"));
        }

        String contentType = file.getContentType();
        String ext = contentType != null ? ALLOWED_IMAGE_TYPES.get(contentType) : null;
        if (ext == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_FILE_TYPE", "Chỉ chấp nhận ảnh JPG, PNG, WEBP, GIF"));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FILE_TOO_LARGE", "File tối đa 10MB"));
        }

        String filename = UUID.randomUUID() + ext;

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);

        String url = baseUrl + "/" + filename;
        log.info("Uploaded file: {}", filename);

        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url, "filename", filename)));
    }
}
