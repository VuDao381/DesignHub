package com.designhub.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chỉ cho phép truy cập tài nguyên tĩnh công khai đối với hình ảnh (preview, avatar)
        // Tuyệt đối KHÔNG cấu hình static handler cho /uploads/files/** để bảo vệ tệp thiết kế gốc
        Path imagesPath = Paths.get(uploadDir, "images").toAbsolutePath().normalize();
        String imagesUri = imagesPath.toUri().toString();

        if (!imagesUri.endsWith("/")) {
            imagesUri += "/";
        }

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(imagesUri);
    }
}
