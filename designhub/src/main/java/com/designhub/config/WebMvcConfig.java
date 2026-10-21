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
        Path imagesPath = Paths.get(uploadDir, "images").toAbsolutePath().normalize();
        String imagesUri = imagesPath.toUri().toString();

        if (!imagesUri.endsWith("/")) {
            imagesUri += "/";
        }

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(imagesUri);
    }
}
