package com.kinhduanpc.service;

import com.kinhduanpc.dto.FileUploadResponse;
import com.kinhduanpc.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080/api/files}")
    private String baseUrl;

    // Whitelist - map content-type to extension
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    public FileUploadResponse uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw AppException.badRequest("EMPTY_FILE", "File không được để trống");
        }

        String contentType = file.getContentType();
        String ext = contentType != null ? ALLOWED_IMAGE_TYPES.get(contentType) : null;
        if (ext == null) {
            throw AppException.badRequest("INVALID_FILE_TYPE", "Chỉ chấp nhận ảnh JPG, PNG, WEBP, GIF");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw AppException.badRequest("FILE_TOO_LARGE", "File tối đa 10MB");
        }

        String filename = UUID.randomUUID() + ext;

        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);

            String url = baseUrl + "/" + filename;
            log.info("Uploaded file: {}", filename);

            return FileUploadResponse.builder()
                    .url(url)
                    .filename(filename)
                    .build();
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw AppException.badRequest("FILE_UPLOAD_FAILED", "Không thể tải file lên");
        }
    }
}
