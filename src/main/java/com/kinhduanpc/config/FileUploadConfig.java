package com.kinhduanpc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@Slf4j
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Phục vụ file upload qua /files/**
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) log.info("Created upload directory: {}", dir.getAbsolutePath());
        }
        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:" + dir.getAbsolutePath() + "/");
    }
}
